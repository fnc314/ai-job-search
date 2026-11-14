#!/usr/bin/env python3
"""DEPRECATED: Supply chain security — retained as Python, shell replacement sufficient.

Supply-chain guards for the template's riskiest surfaces.

Run from anywhere: python scripts/security_guards.py

This repo ships pre-approved Claude Code permissions and CLI code that every
fork user executes. These guards make the dangerous changes LOUD, not
impossible: a PR that intentionally needs one of them must update the
allowlists in this file in the same diff, so the change is explicit and
reviewable rather than buried.

Checks:
1. .claude/settings.json — every permissions.allow entry must be in the exact
   allowlist below. Catches permission widening (e.g. Bash(*), Bash(curl:*)),
   which would auto-approve commands on every fork.
2. .gitignore — the personal-data ignore rules must all still be present,
   and no un-allowlisted negation (!pattern) may re-include them. Catches
   weakening that would make future users silently commit their tracker,
   profile exports, or application archives.
3. .agents/**/package.json — no npm/bun lifecycle scripts (preinstall,
   install, postinstall, prepare, prepack) and no trustedDependencies.
   Catches code execution smuggled into `bun install`.

Stdlib only. Exit 0 on success, 1 with a failure list otherwise.
"""

import ast
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
errors: list[str] = []

# The exact permission entries the template ships. A PR that adds or changes
# an entry must add it here too - that is the point: the diff shows both.
ALLOWED_PERMISSIONS = {
    "Skill(job-application-assistant)",
    "Bash(bun run:*)",
    "Bash(mise run:*)",
    "Bash(mise:*)",
    "Bash(pdftotext:*)",
}

# Personal-data ignore rules that must never disappear from .gitignore.
REQUIRED_IGNORE_RULES = [
    "salary_data.json",
    # Depth-independent: the job-scraper skill resolves `job-scraper/` relative
    # to its own directory, so the state file lands under .claude/skills/... and
    # a repo-rooted rule silently fails to match it.
    "**/job-scraper/seen_jobs.json",
    "templates/cv/main_*.*",
    "!templates/cv/main_example.tex",
    "templates/cover-letters/cover_*.*",
    "templates/cover-letters/Cover_*.*",
    "!templates/cover-letters/cover_example.tex",
    "job-applications/**",
    "archive/*.csv",
    # Service-account credentials for the tracker store. A committed key grants
    # read/write to the only authoritative copy of the application record.
    "sa.json",
    "**/sa.json",
    "**/*serviceAccount*.json",
    "**/service-account*.json",
]

# Negation (re-include) rules the template legitimately ships. .gitignore is
# order-sensitive: a later `!pattern` re-includes a path an earlier rule
# excluded, so a rule can be physically present in REQUIRED_IGNORE_RULES yet
# no longer ignored (e.g. adding `!salary_data.json`). Set membership on the
# required rules cannot see that. Any negation outside this allowlist is a
# failure - add an intentional one here in the same PR, exactly as with
# ALLOWED_PERMISSIONS, so the widening is explicit and reviewable.
ALLOWED_IGNORE_NEGATIONS = {
    "!templates/cover-letters/OpenFonts/fonts/**",
    "!templates/cv/main_example.tex",
    "!templates/cover-letters/cover_example.tex",
    "!.env.example",
    # Instruction pointers only. The rest of each runner directory stays local.
    "!.gemini/GEMINI.md",
    "!.qwen/QWEN.md",
}

FORBIDDEN_SCRIPTS = {"preinstall", "install", "postinstall", "prepare", "prepack"}


def check_permissions() -> None:
    path = ROOT / ".claude" / "settings.json"
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f".claude/settings.json: unreadable or invalid JSON: {exc}")
        return
    if not isinstance(data, dict):
        errors.append(".claude/settings.json: top-level JSON value must be an object")
        return
    permissions = data.get("permissions", {})
    if not isinstance(permissions, dict):
        errors.append(".claude/settings.json: permissions must be an object")
        return
    allow = permissions.get("allow", [])
    if not isinstance(allow, list) or not all(isinstance(entry, str) for entry in allow):
        errors.append(".claude/settings.json: permissions.allow must be a list of strings")
        return
    for entry in allow:
        if entry not in ALLOWED_PERMISSIONS:
            errors.append(
                f".claude/settings.json: permission not in the reviewed allowlist: {entry!r}. "
                "Pre-approved permissions run without prompting on every fork. If this entry is "
                "intentional, add it to ALLOWED_PERMISSIONS in scripts/security_guards.py in the "
                "same PR so the widening is explicit and reviewable."
            )
    for entry in ALLOWED_PERMISSIONS - set(allow):
        # Not an error: settings may legitimately drop an entry. But an
        # allowlist entry that no longer exists should be pruned.
        print(f"note: allowlisted permission not present in settings.json: {entry!r}")


def check_gitignore() -> None:
    path = ROOT / ".gitignore"
    try:
        lines = [line.strip() for line in path.read_text(encoding="utf-8").splitlines()]
    except OSError as exc:
        errors.append(f".gitignore: unreadable: {exc}")
        return
    rules = set(lines)
    for rule in REQUIRED_IGNORE_RULES:
        if rule not in rules:
            errors.append(
                f".gitignore: required personal-data rule missing: {rule!r}. "
                "These rules keep fork users from committing personal data. If the rule moved "
                "or was renamed intentionally, update REQUIRED_IGNORE_RULES in "
                "scripts/security_guards.py in the same PR."
            )
    for line in lines:
        if line.startswith("!") and line not in ALLOWED_IGNORE_NEGATIONS:
            errors.append(
                f".gitignore: negation rule not in the reviewed allowlist: {line!r}. "
                "A negation re-includes a path an earlier rule excluded and can silently "
                "re-expose personal data (a required ignore rule stays present but stops "
                "taking effect). If this negation is intentional, add it to "
                "ALLOWED_IGNORE_NEGATIONS in scripts/security_guards.py in the same PR."
            )


def check_package_manifests() -> None:
    manifests = [
        p for p in ROOT.glob(".agents/**/package.json") if "node_modules" not in p.parts
    ]
    if not manifests:
        errors.append(".agents: no package.json files found - glob roots are wrong or the tree moved")
    for manifest in manifests:
        relpath = manifest.relative_to(ROOT)
        try:
            data = json.loads(manifest.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{relpath}: unreadable or invalid JSON: {exc}")
            continue
        if not isinstance(data, dict):
            errors.append(f"{relpath}: top-level JSON value must be an object")
            continue
        scripts = data.get("scripts", {})
        if not isinstance(scripts, dict):
            errors.append(f"{relpath}: scripts must be an object")
            continue
        bad = FORBIDDEN_SCRIPTS & set(scripts)
        if bad:
            errors.append(
                f"{relpath}: lifecycle script(s) {sorted(bad)} are forbidden - they execute "
                "arbitrary code during `bun install` on every fork user's machine."
            )
        if "trustedDependencies" in data:
            errors.append(
                f"{relpath}: trustedDependencies is forbidden - it re-enables dependency "
                "lifecycle scripts that bun blocks by default."
            )


# ── Interface-first naming (AGENTS.md "Interface-First Naming") ───────────────
# Public names describe the role (Remote, Local, Network, Blob, Email), never the
# tool. Anything that does work is deployable and therefore public. See
# docs/interface-naming-exceptions.md. `sheet` is generic and intentionally allowed.
NAMING_SUBSTRINGS = ("firestore", "firebase", "gmail", "composio", "kermit", "jpype")
NAMING_TOKENS = {"gcs", "room", "ktor", "notion"}
NAMING_BASELINE = ROOT / "docs" / "interface-naming-baseline.txt"
NAMING_EXEMPT_PATH_PARTS = ("KmpInspector", "/impl/", "/build/", "/test/", "/tests/", "Test/", "/node_modules/")
KOTLIN_PUBLIC_DECL = re.compile(
    r"^\s*public\s+(?:(?:data|sealed|abstract|open|enum|annotation|value|inline|expect|actual|"
    r"override|suspend|operator|const|lateinit|tailrec|infix|companion|fun)\s+)*"
    r"(?:fun|class|interface|object|val|var)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)"
)
CAMEL_TOKEN = re.compile(r"[A-Z]+(?=[A-Z][a-z])|[A-Z]?[a-z]+|[A-Z]+|\d+")


def names_a_tool(name: str) -> bool:
    lowered = name.lower()
    if any(s in lowered for s in NAMING_SUBSTRINGS):
        return True
    return any(t.lower() in NAMING_TOKENS for t in CAMEL_TOKEN.findall(name.replace("_", " ")))


def _naming_exempt(rel: str) -> bool:
    return any(part in f"/{rel}" for part in NAMING_EXEMPT_PATH_PARTS)


def collect_naming_violations() -> set[str]:
    found: set[str] = set()
    for base in ("frontend", "records", "backend", "gradle/tools/project-plugins"):
        for path in (ROOT / base).rglob("*.kt"):
            rel = path.relative_to(ROOT).as_posix()
            if _naming_exempt(rel):
                continue
            if names_a_tool(path.stem):
                found.add(f"{rel}:<file>")
            for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
                m = KOTLIN_PUBLIC_DECL.match(line)
                if m and names_a_tool(m.group(1)):
                    found.add(f"{rel}:{m.group(1)}")
    for path in (ROOT / "scripts").glob("*.py"):
        rel = path.relative_to(ROOT).as_posix()
        if names_a_tool(path.stem):
            found.add(f"{rel}:<file>")
        try:
            tree = ast.parse(path.read_text(encoding="utf-8"))
        except (OSError, SyntaxError):
            continue
        stack: list[ast.AST] = [tree]
        while stack:
            node = stack.pop()
            for child in getattr(node, "body", []):
                if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                    if not child.name.startswith("_") and names_a_tool(child.name):
                        found.add(f"{rel}:{child.name}")
                    if isinstance(child, ast.ClassDef):
                        stack.append(child)
    return found


def check_interface_naming() -> None:
    try:
        baseline = {
            line.strip()
            for line in NAMING_BASELINE.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#")
        }
    except OSError:
        baseline = set()
    current = collect_naming_violations()
    for entry in sorted(current - baseline):
        errors.append(
            f"interface-naming: public surface names a tool: {entry}. Name it for the role "
            "(Remote/Local/Network/Blob/Email), or record a justified exception in "
            "docs/interface-naming-exceptions.md."
        )
    for entry in sorted(baseline - current):
        print(f"note: interface-naming baseline entry resolved, prune it: {entry}")


def write_naming_baseline() -> None:
    header = (
        "# Known interface-first naming violations (path:name). Shrink only; see\n"
        "# docs/interface-naming-exceptions.md. Regenerate: mise run dev:guards -- --write-naming-baseline\n"
    )
    NAMING_BASELINE.write_text(header + "\n".join(sorted(collect_naming_violations())) + "\n", encoding="utf-8")
    print(f"wrote {NAMING_BASELINE}")


def main() -> int:
    if "--write-naming-baseline" in sys.argv:
        write_naming_baseline()
        return 0
    check_permissions()
    check_gitignore()
    check_package_manifests()
    check_interface_naming()
    if errors:
        print(f"security_guards: {len(errors)} failure(s)")
        for err in errors:
            print(f"  - {err}")
        return 1
    print("security_guards: OK (permissions allowlist, gitignore rules, package manifests, interface naming)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
