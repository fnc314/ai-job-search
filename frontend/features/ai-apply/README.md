# Feature: AI Apply

> [!NOTE]
> **Quick Facts**
> - **Gradle Project IDs**: `:frontend:features-ai-apply-domain`, `:frontend:features-ai-apply-data`, `:frontend:features-ai-apply-ui`
> - **Module Tier**: Feature / User Experience (Clean Architecture: Domain, Data, UI)
> - **Package Base**: `dev.fnc314.aijobtracker.frontend.features.aiapply`
> - **Target Platforms**: Android (`androidTarget`), Desktop JVM (`jvm`), iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`), Web (`js`, `wasmJs`)
> - **Internal Project Dependencies**:
>   - Domain: `:frontend:core-data`
>   - Data: `:frontend:features-ai-apply-domain`, `:frontend:core-data`, `:frontend:core-network`
>   - UI: `:frontend:features-ai-apply-domain`, `:frontend:features-ai-apply-data`, `:frontend:core-artifacts`, `:frontend:core-widgets`, `:frontend:core-navigation`, `:frontend:core-data`, `:frontend:features-artifact-previewer-ui`

---

## Overview

The `features/ai-apply` module powers the interactive **Apply Assistant** top-level destination (`Edits` / `Pencil` navigation slot). It coordinates dual-tier job fit scoring (instant deterministic `RankScorer` baseline + asynchronous Tier 2 AI semantic calibration), background LaTeX/PDF artifact drafting, Room persistence across process death, and real-time remote application status progression (`RANKED` $\rightarrow$ `DRAFTING` $\rightarrow$ `DRAFTED` $\rightarrow$ `SUBMITTED`).

---

## Architectural Breakdown & Subprojects

### 1. `:features-ai-apply-domain`
Defines immutable workflow models, repository contract, and SAM use cases:
- `ApplyAssistantRepository`
- `ObserveApplySessionUseCase`, `EvaluateJobPostingUseCase`, `RefineJobEvaluationWithAiUseCase`, `GenerateApplicationArtifactsUseCase`, `ObserveArtifactJobUseCase`, `SyncDraftedArtifactsUseCase`, `ConfirmApplicationSubmissionUseCase`, `ResetApplySessionUseCase`
- `aiApplyDomainModule`

### 2. `:features-ai-apply-data`
Provides local Room persistence, HTTP network endpoints (`/applications/apply`, `/ai/rank`, `/artifacts/create`, `/artifacts/jobs/{jobId}`), on-device HTML fallback metadata parsing, and remote listener/status synchronization:
- `LocalApplyAssistantDataSourceImpl`, `NetworkApplyAssistantDataSourceImpl`, `RemoteApplyAssistantDataSourceImpl`
- `ApplyAssistantRepositoryImpl`
- `aiApplyDataModule`

### 3. `:features-ai-apply-ui`
Renders the single-view interactive chat workflow and MVI coordination:
- `ApplyAssistantViewModel` (`ApplyAssistantUiState`, `ApplyAssistantUiIntent`)
- `AiApplyScreen` (animated dual-tier score transition, interactive Yes/No confirmations, artifact links)
- `AiApplyRoute`, `AiApplyEntryRegistry`, `aiApplyUiModule`, `aiApplyFeatureModule`

---

## Key Gradle Tasks

Run the following commands from the repository root:

```bash
# Run unit tests
./gradlew -p frontend :features-ai-apply-domain:jvmTest \
                      :features-ai-apply-data:jvmTest \
                      :features-ai-apply-ui:jvmTest

# Run Detekt static analysis
./gradlew -p frontend :features-ai-apply-domain:detekt \
                      :features-ai-apply-data:detekt \
                      :features-ai-apply-ui:detekt
```
