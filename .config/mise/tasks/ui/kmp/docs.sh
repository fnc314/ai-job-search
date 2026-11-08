#!/usr/bin/env sh
#MISE description="Generate unified multi-module Dokka API documentation HTML for ui/kmp"
#MISE alias="ui:kmp:docs:generate"
set -eu
cd ui/kmp && ./gradlew :dokkaGenerate
