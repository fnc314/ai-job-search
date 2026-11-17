## `artifacts:add`

Register and upload an artifact file to GCS


- **Usage:** `artifacts:add [FLAGS] <id>`

### Arguments
- **`<id>`** — Application ID

### Flags
- **`--kind <kind>`** — Artifact type (e.g. artifact:cv)
- **`--path <path>`** — Local file path
- **`--filename <filename>`** — Override stored filename
- **`--no-upload`** — Record the path only
- **`--dry-run`** — Preview without writing