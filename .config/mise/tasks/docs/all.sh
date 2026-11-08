#!/usr/bin/env sh
#MISE description="Generate all workspace documentation (Dokka, README validations, task docs)"
#MISE alias="docs:all"
set -eu

mise run ui:kmp:docs:generate
mise run records:docs:generate
mise run ui:kmp:docs:readmes
mise run docs:tasks
