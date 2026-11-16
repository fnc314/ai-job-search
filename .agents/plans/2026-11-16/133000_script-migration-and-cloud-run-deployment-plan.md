# Architecture & Implementation Plan: Script Migration, Containerized TeX Live & Cloud Run Deployment

**Status:** Completed
**Date:** 2026-11-16
**Target:** `backend/`, `scripts/`, `deploy/`, and Cloud Run infrastructure
**Depends On:** `.agents/plans/2026-11-16/054800_mobile-migration-and-on-device-intelligence-plan.md`

---

## 1. Goal Description

Following the creation of the backend routing backbone and the Compose Multiplatform in-app PDF rendering engine, our backend service currently runs only locally, and several operational components still rely on legacy local Python scripts and desktop TeX tools:
1. **Scraping Engine (`scripts/scrape.py`):** Spawns local CLIs and reads local `search-queries.md` rather than executing as pure Kotlin Ktor clients in `:backend:job-post-scraping`.
2. **LaTeX Compiler Environment:** `backend/Dockerfile` contains only a slim JRE (`eclipse-temurin:21-jre`), lacking TeX tools (`pdflatex`, `xelatex`, fonts) to actually compile LaTeX on the server.
3. **Cloud Run Deployment:** The backend has never been deployed live, meaning mobile devices (Galaxy S22 Ultra, Z Fold6, Tab S10 FE) cannot reach the backend over cellular or external Wi-Fi.

**Objective:**
1. Migrate the portal search and scraping logic into `:backend:job-post-scraping`.
2. Package containerized TeX Live into `backend/Dockerfile` and implement `LatexCompilerEngine` in `:backend:artifact-compilation`.
3. Wire Cloud Gemini 1.5 Flash client into `POST /applications/apply` for automated CV/Cover Letter tailoring.
4. Deploy the backend service to Google Cloud Run via `mise run remote:deploy:service` so mobile devices can access live scraping, ranking, tailoring, and PDF compilation from anywhere.

---

## 2. Evaluation & Design Details

### 2.1 Containerized TeX Live Strategy
- *Problem:* Full `texlive-full` is ~5 GB, which bloats container builds and cold starts.
- *Solution:* Multi-stage or optimized Debian/Alpine Docker container installing only:
  - `texlive-latex-base`
  - `texlive-latex-extra`
  - `texlive-fonts-recommended` (including `inconsolata`, `tgheros`, `lato`)
  - `texlive-xetex`
- Total footprint: **~280 MB - 350 MB**, keeping Cloud Run cold starts under 4 seconds while supporting identical LaTeX styling from `templates/cv/main_example.tex` and `cover.cls`.

### 2.2 Portal Scraping Architecture (`:backend:job-post-scraping`)
Instead of spawning external Bun processes:
- Implement direct Ktor HTTP clients fetching public aggregator APIs:
  - **FreeHire API:** Public REST API returning normalized job postings.
  - **LinkedIn Guest Search:** Structured HTML/JSON parser using Ksoup or Regex.
  - **Danish Portals (Jobnet / Jobindex / Jobdanmark):** Direct HTTP API integrations.
- Emits unified `Flow<ScrapedJobPosting>` directly to the route handler.

### 2.3 Cloud Run Deployment & Cost Model
- **Scale-to-Zero Compute:** Minimum instances = 0; Maximum instances = 1–2.
- **Cost:** $0.00 / month (well within Google Cloud Run's free tier of 2M requests, 360,000 vCPU-seconds, and 180,000 GiB-seconds).
- **Environment & Secrets:** Ingests `TRACKER_PROJECT`, `TRACKER_SA_KEY` or Application Default Credentials (ADC), and `GEMINI_API_KEY`.

---

## 3. Proposed Changes

### Component 1: `backend/Dockerfile`
#### [MODIFY] `backend/Dockerfile`
- Upgrade base image to include `texlive-latex-base`, `texlive-latex-extra`, `texlive-fonts-recommended`, and `texlive-xetex`.
- Ensure `pdflatex` and `xelatex` binaries are on the PATH and executable.

### Component 2: `:backend:artifact-compilation`
#### [NEW] `LatexCompilerEngine.kt`
- Implements `DocumentCompilerEngine`:
  - Writes `.tex` to a temporary directory.
  - Spawns `pdflatex` or `xelatex` subprocess.
  - Verifies exit code and checks output `.pdf`.
  - Emits `CompilationResult` with byte stream or uploaded storage URL.

### Component 3: `:backend:job-post-scraping`
#### [NEW] `FreeHirePortalScraper.kt` & `LinkedInPortalScraper.kt`
- Implements `PortalScraper` using Ktor HTTP client.
- Parses job titles, descriptions, companies, and URLs without Python or Bun dependencies.

### Component 4: Deployment & Remote Execution
#### [EXECUTE] `mise run remote:deploy:service`
- Builds and deploys container to Cloud Run.
- Validates `/health` check on the live Cloud Run URL.

---

## 4. Phased Execution Steps

1. **Phase 1: Container & TeX Compiler Engine**
   - Update `backend/Dockerfile` with lightweight TeX Live packages.
   - Implement `LatexCompilerEngine.kt` in `:backend:artifact-compilation`.
   - Test LaTeX compilation locally via Docker or native binary.
2. **Phase 2: Kotlin Portal Scraping**
   - Implement `FreeHirePortalScraper` in `:backend:job-post-scraping`.
   - Wire scraper into `JobScraperService` and verify `POST /jobs/scrape`.
3. **Phase 3: Deploy to Cloud Run**
   - Authenticate and trigger `mise run remote:deploy:service`.
   - Record public HTTPS service URL and verify `/health` and `/jobs/scrape` over cellular network from Galaxy S22 Ultra.

---

## 5. Verification Plan

### Automated Tests
```bash
./gradlew :backend:artifact-compilation:test
./gradlew :backend:job-post-scraping:test
./gradlew :backend:service:test
```

### Cloud Run Live Verification
```bash
ENDPOINT=$(gcloud run services describe job-tracker-backend --platform=managed --region=us-east1 --format="value(status.url)")
curl -X GET "$ENDPOINT/health"
curl -X POST "$ENDPOINT/jobs/rank" -H "Content-Type: application/json" -d '{"text": "Android Kotlin Jetpack Compose Engineer"}'
```

### Mobile Verification
- Open mobile app on **Galaxy S22 Ultra** over cellular data.
- Trigger scrape and rank against the live Cloud Run endpoint.
- Verify PDF CV rendering in-app.
