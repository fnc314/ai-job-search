#!/usr/bin/env sh
#MISE description="Generate Zsh completions from task Usage specs and install to ~/.env/zsh/custom/completions/"
#MISE alias="completions:generate"
set -eu

completions_dir="$HOME/.env/zsh/custom/completions"
if mkdir -p "$completions_dir" 2>/dev/null && touch "${completions_dir}/_mise.test" 2>/dev/null; then
    rm -f "${completions_dir}/_mise.test"
    mise completion zsh > "${completions_dir}/_mise"
    echo "Generated Zsh completions in $completions_dir"
else
    mkdir -p ./completions
    mise completion zsh > ./completions/_mise
    echo "Generated Zsh completions in ./completions/_mise (user directory unwritable)"
fi
