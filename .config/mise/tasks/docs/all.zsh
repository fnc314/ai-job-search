#!/usr/bin/env zsh
#MISE description="Generate all workspace documentation (Dokka, README validations, task docs)"
#MISE alias="docs:all"
set -eu

mise run docs:generate
mise run frontend:docs:readmes
mise run docs:tasks
