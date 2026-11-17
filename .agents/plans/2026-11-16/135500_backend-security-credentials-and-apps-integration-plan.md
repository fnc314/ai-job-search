# Implementation Plan: Cloud Run Security, Firebase Credentials Protection, and Frontend Apps Integration

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** repository-wide (`backend`, `frontend`, infrastructure)  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** `7093357f-5e5a-45e0-9093-a854e5ab458a`  
**Depends On:** [`./054800_mobile-migration-and-on-device-intelligence-plan.md`](./054800_mobile-migration-and-on-device-intelligence-plan.md), [`./133000_script-migration-and-cloud-run-deployment-plan.md`](./133000_script-migration-and-cloud-run-deployment-plan.md)  

---

## 1. Executive Summary & Goals

This plan addresses three critical requirements:
1. **Cloud Run Security Layer:** Securing the deployed Cloud Run service (`https://job-tracker-backend-399944190604.us-east1.run.app`) from public/unauthorized internet traffic while allowing authorized client requests from `@frontend/apps` and local CLIs.
2. **Firebase Client Credentials Protection:** Explaining the threat model of `google-services.json` and `GoogleService-Info.plist`, demonstrating why current Firestore rules already block unauthorized ingress, and obscuring/sanitizing these files in git using tracked templates (`*.example`) and gitignored live configurations.
3. **`@frontend/apps` Integration:** Connecting the Compose Multiplatform UI (`frontend/apps/core`, `android`, `ios`, `desktop`) with the live Cloud Run backend routes (`/jobs/scrape`, `/jobs/rank`, `/applications/apply`, `/compiler/compile`) and the native on-device PDF renderer.

---

## 2. Threat Model & Analysis

### 2.1 Firebase Client Configuration Files (`google-services.json` & `GoogleService-Info.plist`)
- **What they contain:** Firebase Project ID (`job-search-fnc314-dev`), Storage Bucket, Client App ID (`mobilesdk_app_id`), and a client Web API key (`current_key`).
- **Is this an open backdoor to Firestore?**
  - **NO.** In Firebase's security model, client API keys identify the Firebase project; they do **not** grant administrative privileges or bypass security rules.
  - In `firestore.rules` (at project root and deployed to Firebase):
    ```javascript
    service cloud.firestore {
      match /databases/{database}/documents {
        match /{document=**} {
          allow read, write: if false;
        }
      }
    }
    ```
  - Because `allow read, write: if false;` is strictly enforced, **any client request attempting to query or modify Firestore directly using `google-services.json` is unconditionally rejected with HTTP 403 Forbidden**.
  - Only the Backend / Admin SDK (using GCP Service Account credentials / Application Default Credentials) can read or write Firestore.
- **Why hide/obscure them anyway?**
  - Public scanners (e.g., GitHub secret scanners, trufflehog) flag Google API keys even when restricted.
  - Quota exhaustion or nuisance spam against Firebase Auth/Analytics endpoints if keys are scraped.
  - Obscuring strategy:
    1. Create sanitized template files: `frontend/apps/android/google-services.json.example` and `frontend/apps/ios/iosApp/GoogleService-Info.plist.example`.
    2. Add `frontend/apps/android/google-services.json` and `frontend/apps/ios/iosApp/GoogleService-Info.plist` to `.gitignore`.
    3. Untrack the live files from git (`git rm --cached`).
    4. Provide automated gradle fallback so local builds without the file use dummy mock configurations or pull from `.env.local`.

### 2.2 Cloud Run Ingress & Endpoint Security
- **Current State:** Cloud Run service was deployed with `--allow-unauthenticated` so external HTTP clients can reach it without Google IAM tokens.
- **The Risk:** While `GET /health` is harmless, mutation routes (`POST /jobs/scrape`, `POST /jobs/rank`, `POST /applications/apply`, `POST /compiler/compile`) invoke TeX live compilation, external web scraping, and LLM ranking. An unauthorized attacker could trigger resource exhaustion or billable CPU cycles.
- **Solution:**
  - **Shared Secret / API Key Interceptor (`X-Api-Key`):**
    - Implement a Ktor `Authentication` plugin or HTTP interceptor in `backend/service`.
    - `GET /health` remains unauthenticated (for Cloud Run uptime health checks and monitoring).
    - All API routes under `/api/*` (or `/jobs/*`, `/applications/*`, `/compiler/*`) require header `X-Api-Key: <SECRET_KEY>` matching the `JOB_TRACKER_API_KEY` environment variable.
    - Cloud Run receives this secret securely via Secret Manager / environment variable.
    - `@frontend/apps` passes this key via `frontend/core/network` Ktor HttpClient headers.

---

## 3. Architecture & Implementation Steps

### Phase 1: Cloud Run Security Layer (`backend/service`)
1. **API Key Authentication Plugin:**
   - Create `dev.fnc314.aijobtracker.backend.security.ApiKeyAuth.kt` in `:backend:service`.
   - Validate `X-Api-Key` header against `System.getenv("JOB_TRACKER_API_KEY")`.
   - In production, reject missing/invalid keys with HTTP 401 Unauthorized `{"error": "Unauthorized"}`.
   - If `JOB_TRACKER_API_KEY` is not set in local dev, log a warning and allow local calls (or default to a known dev secret).
2. **Apply Security to Routes:**
   - In `backend/service/src/main/kotlin/dev/fnc314/aijobtracker/backend/Main.kt`, group protected routes inside an authenticated routing block:
     ```kotlin
     authenticate("api-key") {
         scrapingRoutes(scraperService)
         rankingRoutes()
         applicationRoutes()
         compilerRoutes(compilerService)
     }
     ```
3. **Deploy & Update Cloud Run:**
   - Generate secure random token (e.g. 64-char hex) and store in Google Secret Manager or Cloud Run env var `JOB_TRACKER_API_KEY`.
   - Update `.github/workflows/deploy-backend.yml` to support secret injection.

### Phase 2: In-Repo Encrypted Credentials Solution (`secrets:encrypt` / `secrets:decrypt`)
1. **Design & Tooling:**
   - Use standard AES-256-CBC (`openssl enc`) or `gpg --symmetric` via `mise` tasks (`secrets:encrypt` and `secrets:decrypt`).
   - Package `frontend/apps/android/google-services.json` and `frontend/apps/ios/iosApp/GoogleService-Info.plist` into an encrypted archive: `credentials.tar.gz.enc`.
2. **Add Mise Tasks:**
   - `.config/mise/tasks/secrets/encrypt`: Creates `credentials.tar.gz.enc` prompted by passphrase or `SECRETS_PASSPHRASE` from `.env.local`.
   - `.config/mise/tasks/secrets/decrypt`: Unpacks `credentials.tar.gz.enc` back to the exact project locations.
3. **Template Fallbacks & Git Protection:**
   - Create template examples: `frontend/apps/android/google-services.json.example` and `frontend/apps/ios/iosApp/GoogleService-Info.plist.example`.
   - Add the unencrypted live files to `.gitignore`.
   - Untrack live files from git: `git rm --cached frontend/apps/android/google-services.json frontend/apps/ios/iosApp/GoogleService-Info.plist`.
   - Commit only `credentials.tar.gz.enc` and the `.example` templates.
4. **CI/Build Automation:**
   - In GitHub Actions (`.github/workflows/ci.yml`), decrypt `credentials.tar.gz.enc` using `${{ secrets.SECRETS_PASSPHRASE }}` before compiling Android/iOS.
   - If `SECRETS_PASSPHRASE` is missing (e.g. fork PRs), fallback to copying `.example` files so compilation succeeds.

### Phase 3: `@frontend/apps` Integration
1. **Configure Network Client with Remote Endpoint & Auth:**
   - In `frontend/core/network`, configure the shared Ktor `HttpClient`:
     - Base URL: `https://job-tracker-backend-399944190604.us-east1.run.app` (configurable via `BuildKonfig` or environment).
     - Default request header: `X-Api-Key: <configured_api_key>`.
2. **Connect UI Features:**
   - Ensure `frontend/features/job-applications-list` and `frontend/features/add-job-application` call `BackendApplicationRepository` / `BackendScraperClient` to trigger live scrapes and rank evaluations.
   - Ensure `ArtifactPreviewScreen` loads generated PDFs into the multiplatform `PdfViewer` component implemented in Option B.
3. **Verify Build Targets:**
   - Verify Android app builds: `./gradlew :frontend:apps:android:assembleDebug`.
   - Verify JVM/Desktop runs: `./gradlew :frontend:apps:desktop:run`.

---

## 4. Verification Plan & Testing Checklist

- [ ] **Cloud Run Auth Test:**
  - `curl -s https://job-tracker-backend-399944190604.us-east1.run.app/health` returns 200 OK without header.
  - `curl -s -X POST https://job-tracker-backend-399944190604.us-east1.run.app/jobs/rank` without key returns 401 Unauthorized.
  - `curl -s -X POST -H "X-Api-Key: <valid_key>" https://job-tracker-backend-399944190604.us-east1.run.app/jobs/rank` returns 200 OK.
- [ ] **Firebase Credentials Protection Test:**
  - `git status` verifies `google-services.json` and `GoogleService-Info.plist` are untracked and ignored.
  - Android build succeeds locally.
- [ ] **Frontend Apps Integration Test:**
  - Assemble Android app: `./gradlew :frontend:apps:android:assembleDebug`.
  - Compile Desktop app: `./gradlew :frontend:apps:desktop:compileKotlinJvm`.
  - Run Konsist architectural tests: `mise run dev:konsist`.

---

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/2026-11-16/135500_backend_security_credentials_and_apps_integration_plan.md`
- [ ] Registry `.agents/plans/README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the plan
- [ ] Push `git push origin main`
