#!/usr/bin/env bash
#MISE description="Verify that a generated PDF has the expected pages and extractable text"
#MISE alias="verify-pdf"
#USAGE arg "<pdf>" help="Path to PDF file"
#USAGE flag "-p --pages <count>" help="Expected number of pages"
#USAGE flag "-m --min-chars <count>" help="Minimum characters expected"
#USAGE flag "-c --contains <text>" help="Substring that must be present in extractable text"

set -euo pipefail

PDF=""
EXPECTED_PAGES=""
MIN_CHARS=1
CONTAINS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        -p|--pages)
            EXPECTED_PAGES="$2"
            shift 2
            ;;
        -m|--min-chars)
            MIN_CHARS="$2"
            shift 2
            ;;
        -c|--contains)
            CONTAINS+=("$2")
            shift 2
            ;;
        *)
            if [[ -z "$PDF" ]]; then
                PDF="$1"
                shift
            else
                echo "Unexpected argument: $1" >&2
                exit 1
            fi
            ;;
    esac
done

if [[ -z "$PDF" ]]; then
    echo "Usage: verify-pdf <pdf> [--pages N] [--min-chars N] [--contains TEXT]" >&2
    exit 1
fi

if [[ ! -f "$PDF" ]]; then
    echo "error: PDF does not exist: $PDF" >&2
    exit 1
fi

if ! command -v pdftotext >/dev/null 2>&1 || ! command -v pdfinfo >/dev/null 2>&1; then
    echo "error: required command 'pdfinfo' or 'pdftotext' was not found; install poppler" >&2
    exit 1
fi

if [[ -n "$EXPECTED_PAGES" ]]; then
    ACTUAL_PAGES=$(pdfinfo "$PDF" | awk '/^Pages:/ {print $2}')
    if [[ "$ACTUAL_PAGES" != "$EXPECTED_PAGES" ]]; then
        echo "error: expected $EXPECTED_PAGES page(s), but found $ACTUAL_PAGES" >&2
        exit 1
    fi
fi

EXTRACTED_TEXT=$(pdftotext "$PDF" -)
CHAR_COUNT=$(echo -n "$EXTRACTED_TEXT" | tr -d '[:space:]' | wc -c | tr -d ' ')

if [[ "$CHAR_COUNT" -lt "$MIN_CHARS" ]]; then
    echo "error: expected at least $MIN_CHARS character(s) of extractable text, but found $CHAR_COUNT" >&2
    exit 1
fi

for NEEDLE in "${CONTAINS[@]}"; do
    if ! echo "$EXTRACTED_TEXT" | grep -Fq "$NEEDLE"; then
        echo "error: missing required text: '$NEEDLE'" >&2
        exit 1
    fi
done

echo "✓ PDF verification passed: $PDF"
