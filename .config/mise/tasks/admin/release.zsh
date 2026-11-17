#!/usr/bin/env zsh
#MISE description="Automate semantic version tag creation (version/X.Y.Z) and release publishing"
#MISE alias="release"
#USAGE arg "[version]" help="Semantic version to release (e.g. 1.0.0)"
#USAGE flag "-p --push" help="Push the commit and created version tag to origin"

set -euo pipefail

VERSION="${1:-}"
PUSH=false

for arg in "$@"; do
    if [[ "$arg" == "-p" || "$arg" == "--push" ]]; then
        PUSH=true
    fi
done

if [[ -z "$VERSION" || "$VERSION" == "-p" || "$VERSION" == "--push" ]]; then
    CURRENT=$(git tag -l "version/*" | sort -V | tail -n1 | sed 's|^version/||')
    echo "Current highest version: ${CURRENT:-None}"
    read -r -p "Enter new version to release (e.g. 1.0.0): " VERSION
fi

VERSION="${VERSION#version/}"
VERSION="${VERSION#v}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "❌ Error: Version '$VERSION' does not follow semantic versioning (X.Y.Z, e.g. 1.0.0)." >&2
    exit 1
fi

TAG_NAME="version/$VERSION"

if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "❌ Error: Git tag '$TAG_NAME' already exists." >&2
    exit 1
fi

if [[ -n $(git status --porcelain) ]]; then
    echo "⚠️ Warning: Working tree has uncommitted changes:" >&2
    git status -s >&2
    echo "Please commit or stash your changes before tagging a release." >&2
    exit 1
fi

BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "🚀 Creating git tag '$TAG_NAME' on branch '$BRANCH'..."
git tag -a "$TAG_NAME" -m "Release version $VERSION"
echo "✓ Created annotated tag: $TAG_NAME"

if [[ "$PUSH" == true ]]; then
    echo "Pushing '$BRANCH' and '$TAG_NAME' to origin..."
    git push origin "$BRANCH"
    git push origin "$TAG_NAME"
    echo "✓ Pushed to remote."
else
    echo "Tag created locally. When ready to publish:"
    echo "   git push origin $BRANCH"
    echo "   git push origin $TAG_NAME"
fi
