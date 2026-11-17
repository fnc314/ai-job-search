## `dev:verify-pdf`

Verify that a generated PDF has the expected pages and extractable text


- **Usage:** `dev:verify-pdf [FLAGS] <pdf>`
- **Aliases:** `verify-pdf`

### Arguments
- **`<pdf>`** — Path to PDF file

### Flags
- **`-p --pages <count>`** — Expected number of pages
- **`-m --min-chars <count>`** — Minimum characters expected
- **`-c --contains <text>`** — Substring that must be present in extractable text