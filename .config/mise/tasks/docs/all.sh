#!/usr/bin/env sh
#MISE description="Generate all workspace documentation (Dokka, README validations, task docs)"
#MISE alias="docs:all"
set -eu

mise run frontend:docs:generate
mise run records:docs:generate
mise run frontend:docs:readmes
mise run docs:tasks
