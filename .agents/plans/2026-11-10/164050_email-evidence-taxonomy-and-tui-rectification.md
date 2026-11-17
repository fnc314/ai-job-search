# Implementation Plan: RM-006 Email Evidence Taxonomy, TUI Rectification & In-Context Inspection

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `backend/`, `frontend/apps/terminal`, `frontend/core/data`, `frontend/features/job-application-details`, `tracker.schema.json`  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** c8ab99d9-9541-44bf-a745-c066dcabd512  
**Evaluates:** Roadmap item RM-006 (Email In-App Analysis & Rectification) & Investigation Plan `162700_firestore_events_migration_investigation.md`  
**Schema Authority:** `tracker.schema.json` & `frontend/core/data`  

---

## 1. Executive Summary & Goals

### 1.1. Motivation & Architectural Decision
Roadmap item **[RM-006]** calls for surfacing evidence emails used behind automated decisions (`from`, `subject`, `body` snippet, `labels`, `date received`) and enabling reconciliation of misattributed messages.

Following our investigation in [`.agents/plans/2026-11-10/162700_firestore_events_migration_investigation.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/2026-11-10/162700_firestore_events_migration_investigation.md) and evaluating UI bloat:
1. **Top-Level `emails` Collection is Valid**: Unlike events (which are strictly 1-to-1 and hash-chained within an application), emails are **cross-cutting, mutable in attribution, and can exist without an application** (e.g. `NOISE`, initial recruiter outreach leads, or unlinked confirmations). A top-level collection `emails/{messageId}` provides clean O(1) lookups and global indexing without polluting application subcollections.
2. **Dedicated 5th Top-Level Navigation Tab is Overkill**: Creating 3 new Gradle submodules (`:features:email-inbox:*`) and a 5th navigation tab across mobile bottom bars and desktop rails adds substantial maintenance overhead for an administrative audit task that occurs primarily during automated sync/ingestion.
3. **The Pragmatic Architecture**:
   - **Operational TUI First**: Fast, keyboard-driven triage and rectification in `frontend/apps/terminal` (`mise run tui --emails` / pressing `e` in `tui`) with full search, status classification, and application reassignment.
   - **Centralized Backend Service**: `BackendService.rectifyEmail` / `POST /emails/rectify` provides transactional guarantees for moving message IDs between applications and updating email state.
   - **In-Context GUI Inspection**: Enhance the existing `RelatedEmailsSection` in `features/job-application-details` to render rich email evidence (sender, subject, snippet, `EmailStatus` badge) directly where the user evaluates the job application.

---

## 2. Architecture & Data Flow

```mermaid
graph TD
    subgraph Data Sources & Backend
        JSON[generated/gmail-inbox.json] --> Backfill[POST /emails/tasks/backfill]
        Backfill --> FirestoreEmails[Firestore: emails collection]
        FirestoreApps[Firestore: applications collection] <--> Backfill
        
        RectifyEndpoint[POST /emails/rectify] --> BackendService[BackendService.rectifyEmail]
        BackendService --> FirestoreEmails
        BackendService <--> FirestoreApps
    end

    subgraph Terminal TUI (apps/terminal)
        TUI[TerminalApp --emails / e mode] --> TUIRectify[Interactive Rectification Loop]
        TUIRectify --> BackendService
    end

    subgraph KMC Compose Multiplatform (apps/core & features/job-application-details)
        FirestoreEmails --> DetailsRepo[JobApplicationDetailsRepository]
        DetailsRepo --> DetailsScreen[JobApplicationDetailsScreen]
        DetailsScreen --> EmailEvidenceCard[Rich Email Evidence Cards with Snippet & Status]
    end
```

---

## 3. Detailed Technical Design

### 3.1. Firestore Schema & Core Data Model

#### Schema Definition (`tracker.schema.json`)
Register top-level collection `emails`:
- **Document ID**: Gmail `messageId` (e.g. `1a1c719c260d87b9`).
- **Fields**:
  - `id`: `string`
  - `from`: `string`
  - `subject`: `string`
  - `snippet`: `string`
  - `labels`: `string[]`
  - `receivedAt`: `string` (ISO timestamp)
  - `status`: `string` (`NOISE`, `ACKNOWLEDGEMENT`, `INTERVIEW_REQUEST`, `INTERVIEW_CONFIRMATION`, `ASSESSMENT`, `OFFER`, `REJECTION`, `CORRESPONDENCE`, `UNCLASSIFIED`)
  - `applicationId`: `string?` (nullable: null if unassigned or marked NOISE)
  - `companyId`: `string?` (derived from linked application)
  - `rectifiedAt`: `string?` (ISO timestamp)
  - `createdAt`: `string` (ISO timestamp)
  - `updatedAt`: `string` (ISO timestamp)

#### Core Model: `dev.fnc314.tools.jobtracker.core.data.model.EmailStatus`
In `frontend/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/EmailStatus.kt`:
- Uses standard enum `name` for persistence and serializing directly into Firestore / JSON without synthetic `wireKey` properties.
- Core data models remain pure data and do not import UI string resources (`Res.string`); localized display labels are resolved in the UI layer (`features/job-application-details/ui`) via `composeResources`.
```kotlin
package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Enumerated lifecycle states and evidentiary classifications for email records.
 */
public enum class EmailStatus {
    UNCLASSIFIED,
    ACKNOWLEDGEMENT,
    INTERVIEW_REQUEST,
    INTERVIEW_CONFIRMATION,
    ASSESSMENT,
    OFFER,
    REJECTION,
    CORRESPONDENCE,
    NOISE;

    public val isNoise: Boolean get() = this == NOISE
    public val isActionable: Boolean get() = this == INTERVIEW_REQUEST || this == OFFER || this == ASSESSMENT

    @Composable
    public fun containerColor(): Color = when (this) {
        UNCLASSIFIED -> MaterialTheme.colorScheme.surfaceVariant
        ACKNOWLEDGEMENT -> MaterialTheme.colorScheme.surfaceContainerHigh
        INTERVIEW_REQUEST -> MaterialTheme.colorScheme.secondaryFixed
        INTERVIEW_CONFIRMATION -> MaterialTheme.colorScheme.primaryFixed
        ASSESSMENT -> MaterialTheme.colorScheme.tertiaryFixed
        OFFER -> MaterialTheme.colorScheme.primaryContainer
        REJECTION -> MaterialTheme.colorScheme.errorContainer
        CORRESPONDENCE -> MaterialTheme.colorScheme.surfaceContainer
        NOISE -> MaterialTheme.colorScheme.surfaceDim
    }

    @Composable
    public fun contentColor(): Color = when (this) {
        UNCLASSIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
        ACKNOWLEDGEMENT -> MaterialTheme.colorScheme.onSurface
        INTERVIEW_REQUEST -> MaterialTheme.colorScheme.onSecondaryFixed
        INTERVIEW_CONFIRMATION -> MaterialTheme.colorScheme.onPrimaryFixed
        ASSESSMENT -> MaterialTheme.colorScheme.onTertiaryFixed
        OFFER -> MaterialTheme.colorScheme.onPrimaryContainer
        REJECTION -> MaterialTheme.colorScheme.onErrorContainer
        CORRESPONDENCE -> MaterialTheme.colorScheme.onSurface
        NOISE -> MaterialTheme.colorScheme.outline
    }

    public companion object {
        public fun fromString(raw: String?): EmailStatus {
            if (raw.isNullOrBlank()) return UNCLASSIFIED
            val normalized = raw.trim().uppercase().replace("-", "_")
            return entries.firstOrNull { it.name == normalized } ?: UNCLASSIFIED
        }
    }
}
```

#### Core Model: `dev.fnc314.tools.jobtracker.core.data.model.Email`
In `frontend/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/Email.kt`:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

/**
 * Domain entity representing an email message serving as evidence for pipeline decisions.
 */
@Immutable
public data class Email(
    public val id: String,
    public val from: String,
    public val subject: String,
    public val snippet: String,
    public val labels: ImmutableList<String> = persistentListOf(),
    public val receivedAt: Instant?,
    public val status: EmailStatus = EmailStatus.UNCLASSIFIED,
    public val applicationId: String? = null,
    public val companyId: String? = null,
    public val rectifiedAt: Instant? = null,
    public val createdAt: Instant = Instant.DISTANT_PAST,
    public val updatedAt: Instant = Instant.DISTANT_PAST,
)
```

---

### 3.2. Backend Endpoints & Service Logic (`backend/`)

#### 1. Ingestion: `POST /emails/tasks/backfill`
In `backend/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/BackendService.kt`:
- Reads `generated/gmail-inbox.json`.
- Correlates message IDs against existing applications and timeline events.
- Performs automatic classification into initial `EmailStatus`:
  - `REJECT` events / rejection phrasing -> `EmailStatus.REJECTION`
  - `OFFER` events / offer phrases -> `EmailStatus.OFFER`
  - `SCHEDULE_INTERVIEW` / calendar confirmation -> `EmailStatus.INTERVIEW_CONFIRMATION`
  - Interview / phone screen invitations -> `EmailStatus.INTERVIEW_REQUEST`
  - "Thank you for applying" -> `EmailStatus.ACKNOWLEDGEMENT`
  - Coding challenge / test -> `EmailStatus.ASSESSMENT`
  - Noise phrases (receipts, billing, alerts) -> `EmailStatus.NOISE`
  - General thread -> `EmailStatus.CORRESPONDENCE`
- Executes batch writes to Firestore `emails/{messageId}`.

#### 2. Rectification: `POST /emails/rectify`
In `BackendService.kt`:
```kotlin
public fun rectifyEmail(
    messageId: String,
    targetApplicationId: String?,
    status: EmailStatus,
    dryRun: Boolean = false,
): Map<String, Any?>
```
- Performs a Firestore transaction:
  1. If `status == EmailStatus.NOISE`, forces `targetApplicationId = null`.
  2. If moving between applications:
     - Removes `messageId` from previous application's `emailMessageIds`.
     - Adds `messageId` to target application's `emailMessageIds` and fetches `companyId`.
  3. Updates `emails/{messageId}` with `status`, `applicationId`, `companyId`, `rectifiedAt = now()`.

#### 3. Task Integration:
- `.config/mise/tasks/tracker/emails/backfill.sh` wrapping `POST /emails/tasks/backfill`.

---

### 3.3. TUI Rectification View (`frontend/apps/terminal`)

Update `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/terminal/Main.kt`:
1. **Interactive Entry Points**:
   - CLI invocation: `apps-terminal --emails` (or `mise run tui --emails`).
   - Interactive prompt command: `e`, `emails` inside the existing `interactiveLoop()`.
2. **Interactive TUI Flow (`EmailsApp`)**:
   - **Email Table**:
     - Columns: `Message ID`, `Date`, `Status (Colored Badge)`, `From`, `Subject`, `Linked Application`
   - **Filter & Search Commands**:
     - `/ <query>`: Text search in subject, sender, snippet, or application ID.
     - `s <status>`: Filter by `EmailStatus` (e.g., `s interview_request`, `s noise`, `s offer`).
     - `unlinked`: Display only emails without an associated application.
     - `noise`: Toggle inclusion of noise messages.
   - **Inspection & Rectification (`r <messageId>`)**:
     - Prints full email details and snippet.
     - Prompts for action:
       - `[1] Change Status`: Dropdown/choice of `EmailStatus`.
       - `[2] Reassign Application`: Prompts for application search or ID, previews matching application, and executes reassignment.
       - `[3] Mark Noise`: Automatically sets `status = NOISE` and unlinks from application.
       - `[b] Back`: Return to email list.
     - Immediately applies update via `BackendService.rectifyEmail`.

---

### 3.4. In-Context GUI Inspection (`frontend/features/job-application-details`)

Rather than adding an isolated 5th tab, upgrade the existing `RelatedEmailsSection` in `JobApplicationDetailsScreen.kt`:
1. **Resource Strings**:
   Add localized status strings in `frontend/features/job-application-details/ui/src/commonMain/composeResources/values/strings.xml`:
   - `email_status_unclassified`: "Unclassified"
   - `email_status_acknowledgement`: "Acknowledgement"
   - `email_status_interview_request`: "Interview Request"
   - `email_status_interview_confirmation`: "Interview Confirmed"
   - `email_status_assessment`: "Assessment"
   - `email_status_offer`: "Offer"
   - `email_status_rejection`: "Rejection"
   - `email_status_correspondence`: "Correspondence"
   - `email_status_noise`: "Noise"
   - Helper extension in UI: `EmailStatus.localizedName(): StringResource`
2. **Rich Evidence Cards**:
   - In `JobApplicationDetailsRepositoryImpl`, fetch the corresponding `Email` records from `emails/{messageId}`.
   - Render rich `ElevatedCard` items containing:
     - `EmailStatus` badge with theme-aware container and content colors.
     - Received date formatted relatively (e.g., `"2 days ago"`).
     - Sender (`from`) and subject in bold.
     - Expandable or 2-line snippet preview of the email content.
3. **In-Context Rectification Action**:
   - Provide an "Unlink" or "Mark Noise" icon button on each email card.
   - Clicking dispatches an intent to `JobApplicationDetailViewModel` to call `BackendService.rectifyEmail(messageId, null, EmailStatus.NOISE)`.
   - The application immediately refreshes, keeping the pipeline record pristine without leaving the screen.

---

## 4. Verification Plan

### Automated Tests
1. **Backend Service Tests**:
   - `BackendServiceTest`: Verify `rectifyEmail` correctly updates status, unlinks from old applications, links to new applications, and sets `NOISE` safely.
   - Command: `./gradlew -p backend test`
2. **Terminal TUI Tests**:
   - `TerminalAppTest`: Verify CLI arguments `--emails` launch `EmailsApp`, and verify search/filter parsing.
   - Command: `./gradlew :frontend:apps-terminal:test`
3. **Frontend Details Tests**:
   - `DetailsUseCasesTest` / `JobApplicationDetailViewModelTest`: Verify email evidence cards render with correct `EmailStatus` colors and snippets.
   - Command: `./gradlew :frontend:features:job-application-details:ui:test`
4. **Konsist Architecture Gates**:
   - `mise run dev:konsist`
   - `mise run dev:detekt`

### Manual Verification
1. **Ingest & Backfill**:
   - Run `mise run tracker:emails:backfill` and check Firestore `emails` documents.
2. **TUI Rectification**:
   - Run `mise run tui --emails`.
   - Select an unlinked email, press `r <id>`, assign to an application, and verify `emailMessageIds` updates on that application.
3. **In-Context GUI Inspection**:
   - Launch desktop app (`./gradlew :frontend:apps:desktop:run`).
   - Open the detail screen for the reassigned application.
   - Confirm the email displays with its formatted sender, date, subject, snippet, and status badge.
