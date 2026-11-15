#!/usr/bin/env python3
#MISE description="Automate semantic version tag creation (version/X.Y.Z) and release publishing"
#MISE alias="release"
#USAGE arg "[version]" help="Semantic version to release (e.g. 1.0.0). If omitted, prompts interactively."
#USAGE flag "-p --push" help="Push the commit and created version tag to origin"
"""Automates semantic version tag creation (version/X.Y.Z) and release publishing."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


def run_cmd(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, capture_output=True, text=True, check=check)


def get_current_tags() -> list[str]:
    res = run_cmd(["git", "tag", "-l", "version/*"], check=False)
    if res.returncode != 0:
        return []
    tags = [line.strip().removeprefix("version/") for line in res.stdout.splitlines() if line.strip()]

    def parse_semver(v: str) -> tuple[int, ...]:
        parts = v.split(".")
        nums = []
        for p in parts:
            digits = re.match(r"^\d+", p)
            nums.append(int(digits.group(0)) if digits else 0)
        return tuple(nums)

    tags.sort(key=parse_semver)
    return tags


def get_current_branch() -> str:
    res = run_cmd(["git", "rev-parse", "--abbrev-ref", "HEAD"], check=True)
    return res.stdout.strip()


def is_working_tree_clean() -> bool:
    res = run_cmd(["git", "status", "--porcelain"], check=True)
    return len(res.stdout.strip()) == 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Create and publish version/X.Y.Z release tag.")
    parser.add_argument("version", nargs="?", default=None, help="Version number (e.g. 1.0.0)")
    parser.add_argument("-p", "--push", action="store_true", help="Push tag and branch to origin")
    args = parser.parse_args()

    tags = get_current_tags()
    current_version = tags[-1] if tags else "None"

    version = args.version
    if not version:
        print("🏷️  Release Tag Automation (version/X.Y.Z)")
        print(f"Current highest version: {current_version}")
        try:
            suggested = "1.0.0" if current_version == "None" else ""
            prompt_str = f"Enter new version to release [{suggested}]: " if suggested else "Enter new version to release: "
            user_input = input(prompt_str).strip()
            if not user_input and suggested:
                version = suggested
            else:
                version = user_input
        except (KeyboardInterrupt, EOFError):
            print("\nAborted.")
            sys.exit(1)

    # Normalize: strip leading 'v' or 'version/' if supplied
    version = version.removeprefix("version/").removeprefix("v")

    if not re.match(r"^\d+\.\d+\.\d+$", version):
        print(f"❌ Error: Version '{version}' does not follow semantic versioning (X.Y.Z, e.g. 1.0.0).", file=sys.stderr)
        sys.exit(1)

    tag_name = f"version/{version}"

    # Verify if tag already exists
    if tag_name in [f"version/{t}" for t in tags]:
        print(f"❌ Error: Git tag '{tag_name}' already exists.", file=sys.stderr)
        sys.exit(1)

    # Check working tree cleanliness
    if not is_working_tree_clean():
        print("⚠️ Warning: Working tree has uncommitted changes:")
        subprocess.run(["git", "status", "-s"])
        print("Please commit or stash your changes before tagging a release.")
        sys.exit(1)

    branch = get_current_branch()
    print(f"🚀 Creating git tag '{tag_name}' on branch '{branch}'...")
    run_cmd(["git", "tag", "-a", tag_name, "-m", f"Release version {version}"])
    print(f"✓ Created annotated tag: {tag_name}")

    should_push = args.push
    if not should_push and sys.stdin.isatty():
        try:
            confirm = input(f"Push branch '{branch}' and tag '{tag_name}' to origin now? [y/N]: ").strip().lower()
            should_push = confirm in ("y", "yes")
        except (KeyboardInterrupt, EOFError):
            should_push = False

    if should_push:
        print(f"Pushing '{branch}' to origin...")
        run_cmd(["git", "push", "origin", branch])
        print(f"Pushing tag '{tag_name}' to origin...")
        run_cmd(["git", "push", "origin", tag_name])
        print("✓ Pushed to remote.")
        print("\n🎉 GitHub Actions workflow triggered:")
        print("   https://github.com/fnc314/ai-job-search/actions")
        print("   Public documentation will deploy to: https://ai-job-tracker.fnc314.dev")
    else:
        print("\nTag created locally. When ready to publish:")
        print(f"   git push origin {branch}")
        print(f"   git push origin {tag_name}")


if __name__ == "__main__":
    main()
