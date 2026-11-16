## `secrets:encrypt`

Encrypt client Firebase credentials (google-services.json & GoogleService-Info.plist) into credentials.tar.gz.enc


- **Usage:** `secrets:encrypt [-p --passphrase <passphrase>]`

### Flags
- **`-p --passphrase <passphrase>`** — Passphrase for symmetric encryption (defaults to SECRETS_PASSPHRASE env var or interactive prompt)