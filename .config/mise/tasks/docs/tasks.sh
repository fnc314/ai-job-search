#!/usr/bin/env sh
#MISE description="Regenerates docs/mise documentation directly from self-documenting task files"
#MISE alias="mise:docs:tasks"
set -eu

mkdir -p ./docs/mise
mise generate task-docs -m -o ./docs/mise
