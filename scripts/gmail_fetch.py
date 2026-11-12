"""DEPRECATED: Email fetch — Composio CLI subprocess wrapper, no Kotlin migration needed.

Fetch Gmail into the payload tracker.py collect consumes.

This is the deterministic half of /gmail-sync, extracted so a scheduler can
run it without an agent. It talks to the same Composio Gmail tools the manual
flow uses, so there is one mailbox access path and one set of credentials.

It writes three files and nothing else:

generated/gmail-inbox.json       — {lookbackFrom, query, messages[]}

generated/gmail-labels.json      — {labels: [{id, name, type}]}

generated/gmail-job-labels.json  — cached root JOB label and descendants

collect is a separate step: this script never stages a proposal and never
touches Firestore. Keeping the split means a failed fetch cannot look like a
quiet mailbox.

Label discovery happens before every fetch:

Gmail's current label inventory is fetched.

The root JOB label and every JOB/... descendant are extracted.

The result is compared with the local JOB-label cache.

The cache is updated when the hierarchy has changed.

The current JOB-label list is used to build the Gmail query.

Gmail has no label:JOB/* wildcard, which is why the label hierarchy must be
expanded into individual query clauses.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import pathlib
import shutil
import subprocess
import sys
import threading
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent

DEFAULT_INBOX = ROOT / "generated" / "gmail-inbox.json"
DEFAULT_LABELS = ROOT / "generated" / "gmail-labels.json"
DEFAULT_JOB_LABELS = ROOT / "generated" / "gmail-job-labels.json"

JOB_LABEL = "JOB"
DEFAULT_LOOKBACK_DAYS = 30

# Composio caps one response at roughly 50 full messages, so the fetcher pages
# in batches of this size instead of asking for everything at once.

PAGE_SIZE = 50
DEFAULT_MAX_RESULTS = 200


class FetchError(RuntimeError):
    """The mailbox could not be read, so no payload was written."""


# ── Spinner ────────────────────────────────────────────────────────────────

_SPINNER_FRAMES = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
_SPINNER_INTERVAL = 0.08


def _spinner_thread(stop_event: threading.Event, label: str) -> None:
    """Render a rotating spinner to stderr until stopped."""
    i = 0
    while not stop_event.is_set():
        frame = _SPINNER_FRAMES[i % len(_SPINNER_FRAMES)]
        sys.stderr.write(f"\r  {frame} {label} ")
        sys.stderr.flush()
        time.sleep(_SPINNER_INTERVAL)
        i += 1
    sys.stderr.write("\r\033[K")  # clear the line
    sys.stderr.flush()


def _with_spinner(label: str, func, *args, **kwargs):
    """Run *func* while showing a spinner, then print the elapsed time."""
    stop = threading.Event()
    t = threading.Thread(target=_spinner_thread, args=(stop, label), daemon=True)
    t.start()
    try:
        start = time.monotonic()
        result = func(*args, **kwargs)
        elapsed = time.monotonic() - start
        return result
    finally:
        stop.set()
        t.join(timeout=1.0)
        sys.stderr.write(f"  {label} ({elapsed:.1f}s)\n")
        sys.stderr.flush()


def composioBin() -> str:
    """Locate the Composio CLI."""

    explicit = os.environ.get("COMPOSIO_BIN")
    if explicit:
        return explicit

    found = shutil.which("composio")
    if found:
        return found

    # launchd starts with a minimal PATH; the default install location is not
    # necessarily on it.
    fallback = pathlib.Path.home() / ".local" / "bin" / "composio"
    if fallback.exists():
        return str(fallback)

    raise FetchError("composio CLI not found. Set COMPOSIO_BIN or install it on PATH.")


def runTool(
    slug: str, arguments: dict, *, timeout: int = 180, verbose: bool = False
) -> dict:
    """Execute one Composio tool and return its data object.

    The CLI stores large results in a temp file and returns a pointer instead
    of the payload, so both shapes are handled.
    """

    command = [
        composioBin(),
        "execute",
        slug,
        "-d",
        json.dumps(arguments),
    ]

    if verbose:
        sys.stderr.write(f"  CLI: {' '.join(command[:4])} ...\n")
        sys.stderr.flush()

    def _run() -> subprocess.CompletedProcess:
        return subprocess.run(
            command,
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )

    try:
        completed = _with_spinner(f"{slug}", _run)
    except subprocess.TimeoutExpired as exc:
        raise FetchError(f"{slug} timed out after {timeout}s") from exc

    if verbose:
        sys.stderr.write(f"  {slug} returned code {completed.returncode}\n")
        sys.stderr.flush()

    if completed.returncode != 0:
        # The CLI writes tool-level failures to stdout and exits 1 with an
        # empty stderr, so prefer whatever JSON it printed over the bare code.
        detail = completed.stdout.strip() or completed.stderr.strip()

        try:
            printed = json.loads(detail) if detail else None
        except json.JSONDecodeError:
            printed = None

        if isinstance(printed, dict):
            detail = json.dumps(printed.get("error") or printed.get("data") or printed)

        raise FetchError(f"{slug} exited {completed.returncode}: {detail[:500]}")

    try:
        envelope = json.loads(completed.stdout)
    except json.JSONDecodeError as exc:
        raise FetchError(
            f"{slug} did not return JSON: {completed.stdout.strip()[:300]}"
        ) from exc

    if envelope.get("error"):
        raise FetchError(f"{slug} failed: {json.dumps(envelope['error'])[:500]}")

    if envelope.get("successful") is False:
        raise FetchError(f"{slug} failed: {json.dumps(envelope)[:500]}")

    if envelope.get("storedInFile"):
        path = envelope.get("outputFilePath")
        if not path or not pathlib.Path(path).exists():
            raise FetchError(f"{slug} reported a file result without a file: {path}")

        envelope = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))

    data = envelope.get("data")
    if not isinstance(data, dict):
        raise FetchError(f"{slug} returned no data object")

    return data


def listLabels(*, verbose: bool = False) -> list[dict]:
    """Fetch the complete current Gmail label inventory."""

    data = runTool("GMAIL_LIST_LABELS", {"user_id": "me"}, verbose=verbose)

    labels = []

    for entry in data.get("labels") or []:
        name = entry.get("name")
        if not name:
            continue

        labels.append(
            {
                "id": str(entry.get("id") or ""),
                "name": str(name),
                "type": str(entry.get("type") or "user"),
            }
        )

    return labels


def selectJobLabels(labels: list[dict]) -> list[dict]:
    """Return the root JOB label and every nested JOB label.

    Gmail represents nested labels using slash-separated names, for example:

        JOB
        JOB/Apple
        JOB/Apple/Recruiting

    The root label is explicitly required so a missing or renamed JOB hierarchy
    fails clearly rather than silently omitting job-related messages.
    """

    prefix = f"{JOB_LABEL}/"

    jobLabels = [
        label
        for label in labels
        if label["name"] == JOB_LABEL or label["name"].startswith(prefix)
    ]

    rootFound = any(label["name"] == JOB_LABEL for label in jobLabels)

    if not rootFound:
        raise FetchError(f'Root Gmail label "{JOB_LABEL}" was not found')

    return sorted(
        jobLabels,
        key=lambda label: label["name"].lower(),
    )


def jobLabelNames(labels: list[dict]) -> list[str]:
    """Return a normalized ordered list of the current JOB hierarchy."""

    return [label["name"] for label in selectJobLabels(labels)]


def readJobLabelCache(path: pathlib.Path) -> list[str] | None:
    """Read the cached JOB-label list.

    Returns None when the cache does not exist or is invalid. An empty list is
    not a valid cache because the root JOB label is always required.
    """

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None
    except json.JSONDecodeError:
        return None

    if not isinstance(data, dict):
        return None

    if data.get("root") != JOB_LABEL:
        return None

    labels = data.get("labels")

    if (
        not isinstance(labels, list)
        or not labels
        or not all(isinstance(name, str) and name for name in labels)
    ):
        return None

    if JOB_LABEL not in labels:
        return None

    return labels


def writeJson(path: pathlib.Path, value: dict) -> None:
    """Write JSON, creating parent directories as needed."""

    path.parent.mkdir(parents=True, exist_ok=True)

    path.write_text(
        json.dumps(value, indent=2) + "\n",
        encoding="utf-8",
    )


def updateJobLabelCache(
    path: pathlib.Path,
    currentLabels: list[str],
    *,
    dryRun: bool,
) -> bool:
    """Compare the current JOB hierarchy with the cache and update if needed.

    Returns True when the cache is missing, invalid, or differs from the
    current Gmail hierarchy.
    """

    cachedLabels = readJobLabelCache(path)
    changed = cachedLabels != currentLabels

    if changed and not dryRun:
        writeJson(
            path,
            {
                "root": JOB_LABEL,
                "labels": currentLabels,
            },
        )

    return changed


def selectLabel(labels: list[dict], requested: str) -> dict:
    """Resolve --label exactly against the current Gmail inventory."""

    wanted = requested.strip().lower()

    for label in labels:
        if label["name"].lower() == wanted or label["id"].lower() == wanted:
            return label

    known = ", ".join(
        sorted(label["name"] for label in labels if label["type"] == "user")
    )

    raise FetchError(
        f'Unknown label "{requested}". ' f"This mailbox has: {known or '(none)'}"
    )


def buildQuery(
    jobLabels: list[str],
    label: dict | None,
    since: str | None,
) -> str:
    """Build the Gmail search query.

    A manually requested `--label` takes precedence over the normal combined
    mailbox query. Otherwise, the query includes Primary, Updates, the root JOB
    label, and every discovered JOB descendant.
    """

    bound = (
        f"after:{since.replace('-', '/')}"
        if since
        else f"newer_than:{DEFAULT_LOOKBACK_DAYS}d"
    )

    if label is not None:
        return f'{bound} label:"{label["name"]}"'

    clauses = [
        "category:primary",
        "category:updates",
    ]

    # jobLabels always includes the root JOB label followed by any descendants.
    clauses.extend(f'label:"{name}"' for name in jobLabels)

    return f"{bound} (" + " OR ".join(clauses) + ")"


def bodyText(message: dict) -> str:
    """Extract the best available text body from a fetched message."""

    text = message.get("messageText")
    if isinstance(text, str) and text.strip():
        return text

    preview = message.get("preview")

    if isinstance(preview, dict):
        preview = preview.get("body")

    if isinstance(preview, str):
        return preview

    return ""


def toInboxMessage(message: dict) -> dict:
    """Convert a Composio Gmail message into the tracker payload format."""

    subject = str(message.get("subject") or "").strip()
    body = bodyText(message).strip()

    snippet = f"{subject}\n\n{body}"[:800] if body else subject[:800]

    received = str(message.get("messageTimestamp") or "")

    if received.endswith("Z"):
        received = received[:-1] + "+00:00"

    return {
        "messageId": str(message.get("messageId") or message.get("id") or ""),
        "subject": subject,
        "sender": str(message.get("sender") or ""),
        "snippet": snippet,
        "receivedAt": received,
    }


def fetchMessages(
    query: str,
    limit: int,
    *,
    pageSize: int = PAGE_SIZE,
    timeout: int = 60,
    verbose: bool = False,
) -> list[dict]:
    """Page through the query until limit messages or the mailbox ends.

    Composio rejects a single response much above 50 full messages with
    "The tool response payload is too large", so one big request cannot work.
    Paging keeps every request under that cap while still covering the whole
    lookback window.
    """

    messages: list[dict] = []
    token = None

    while len(messages) < limit:
        arguments = {
            "user_id": "me",
            "query": query,
            "max_results": max(
                1,
                min(pageSize, limit - len(messages), 50),
            ),
            "verbose": True,
            "include_payload": True,
        }

        if token:
            arguments["page_token"] = token

        remaining = limit - len(messages)
        if verbose:
            print(
                f"Fetching {min(pageSize, remaining)} messages "
                f"(timeout {timeout}s) ...",
                file=sys.stderr,
            )

        data = runTool(
            "GMAIL_FETCH_EMAILS",
            arguments,
            timeout=timeout,
            verbose=verbose,
        )

        batch = data.get("messages") or []
        messages.extend(batch)

        token = data.get("nextPageToken")

        if not batch or not token:
            break

    return messages[:limit]


def normalizeSince(value: str | None) -> str | None:
    """Validate and normalize an optional YYYY-MM-DD date."""

    if not value:
        return None

    try:
        dt.date.fromisoformat(value)
    except ValueError as exc:
        raise FetchError(f"--since must be YYYY-MM-DD, got {value!r}") from exc

    return value


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)

    parser.add_argument(
        "--label",
        help=("fetch only this label " "(validated against this run's Gmail labels)"),
    )

    parser.add_argument(
        "--since",
        help="lookback start date, YYYY-MM-DD",
    )

    parser.add_argument(
        "--max",
        type=int,
        default=DEFAULT_MAX_RESULTS,
        help=(f"total messages to collect, " f"fetched {PAGE_SIZE} at a time"),
    )

    parser.add_argument(
        "--inbox",
        default=str(DEFAULT_INBOX),
        help="inbox payload path",
    )

    parser.add_argument(
        "--labels-out",
        default=str(DEFAULT_LABELS),
        help="complete Gmail label inventory path",
    )

    parser.add_argument(
        "--job-labels-out",
        default=str(DEFAULT_JOB_LABELS),
        help="cached JOB-label hierarchy path",
    )

    parser.add_argument(
        "--timeout",
        type=int,
        default=60,
        help="timeout in seconds per Composio API call (default: 60)",
    )

    parser.add_argument(
        "-v",
        "--verbose",
        action="store_true",
        help="print detailed progress and timing to stderr",
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="read Gmail and compare labels, but write nothing",
    )

    args = parser.parse_args(argv)

    verbose = args.verbose

    try:
        # Always query Gmail for the current label inventory. The local JOB cache
        # is used as a persisted inventory and change detector; Gmail remains
        # authoritative.
        labels = listLabels(verbose=verbose)

        if verbose:
            sys.stderr.write(f"  Found {len(labels)} Gmail labels\n")
            sys.stderr.flush()

        jobLabels = jobLabelNames(labels)

        if verbose:
            sys.stderr.write(f"  JOB labels:\n{',\n'.join(jobLabels)}\n")
            sys.stderr.flush()

        jobLabelsPath = pathlib.Path(args.job_labels_out).expanduser()

        jobLabelsChanged = updateJobLabelCache(
            jobLabelsPath,
            jobLabels,
            dryRun=args.dry_run,
        )

        if verbose and jobLabelsChanged:
            sys.stderr.write("  JOB label cache updated\n")
            sys.stderr.flush()

        label = selectLabel(labels, args.label) if args.label else None

        since = normalizeSince(args.since)

        query = buildQuery(
            jobLabels,
            label,
            since,
        )

        if verbose:
            sys.stderr.write(f"  Query: {query}\n")
            sys.stderr.flush()

        raw = fetchMessages(
            query,
            max(1, min(args.max, 500)),
            timeout=args.timeout,
            verbose=verbose,
        )

    except FetchError as exc:
        print(
            json.dumps(
                {
                    "ok": False,
                    "error": str(exc),
                }
            )
        )
        return 1

    messages = [toInboxMessage(message) for message in raw]

    messages = [message for message in messages if message["messageId"]]

    # Paging can overlap at the boundary if the mailbox changes mid-run.
    seen: set[str] = set()
    unique = []

    for message in messages:
        if message["messageId"] in seen:
            continue

        seen.add(message["messageId"])
        unique.append(message)

    messages = unique

    lookbackFrom = (
        f"{args.since}T00:00:00+00:00"
        if args.since
        else (
            dt.datetime.now(dt.timezone.utc) - dt.timedelta(days=DEFAULT_LOOKBACK_DAYS)
        ).isoformat()
    )

    inbox = {
        "lookbackFrom": lookbackFrom,
        "query": query,
        "messages": messages,
    }

    if not args.dry_run:
        writeJson(
            pathlib.Path(args.inbox).expanduser(),
            inbox,
        )

        writeJson(
            pathlib.Path(args.labels_out).expanduser(),
            {
                "labels": labels,
            },
        )

    print(
        json.dumps(
            {
                "ok": True,
                "dryRun": args.dry_run,
                "query": query,
                "labels": len(labels),
                "jobLabels": len(jobLabels),
                "jobLabelsChanged": jobLabelsChanged,
                "messages": len(messages),
                "inbox": (args.inbox if not args.dry_run else None),
                "labelsFile": (args.labels_out if not args.dry_run else None),
                "jobLabelsFile": (str(jobLabelsPath) if not args.dry_run else None),
            },
            indent=2,
        )
    )

    return 0


if __name__ == "__main__":
    sys.exit(main())
