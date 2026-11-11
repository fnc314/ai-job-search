# Implementation Plan: Markdown Rendering in Artifacts Preview

**Status:** Draft / Proposed  
**Date:** 2026-11-10  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Depends On:** [`.agents/plans/2026-11-10/172200_interactive_artifacts_presentation_and_in_app_viewer_plan.md`](./172200_interactive_artifacts_presentation_and_in_app_viewer_plan.md) (artifact content fetching and inline content storage)  
**Defects Fixed:**
- `MarkdownPreviewDialog` fetches and displays artifact content correctly, but renders it as raw plain text instead of formatted markdown. Headers, bold, lists, code blocks, and links appear as literal markdown syntax characters.

---

## 1. Executive Summary & Goals

The `MarkdownPreviewDialog` in `DetailDialogs.kt` successfully loads artifact content (via the 4-tier fallback chain: inline content → external URL → GCS storage → local filesystem). However, the content is displayed using a plain `Text` composable with `bodyMedium` style, so markdown formatting is not rendered — users see raw markdown syntax instead of formatted output.

**Goal:** Replace the plain `Text` composable with a proper markdown renderer that displays formatted headers, bold/italic text, lists, code blocks, links, tables, and other GFM (GitHub Flavored Markdown) elements.

**Library Choice:** `mikepenz/multiplatform-markdown-renderer` (v0.46.0) — the most mature KMP markdown library with:
- Full platform support: Android, iOS, Desktop (JVM), Web (Wasm/JS), macOS
- Material 3 theming via `multiplatform-markdown-renderer-m3`
- GFM support: tables, task lists, strikethrough, autolinks, GitHub alerts
- Async parsing with `rememberMarkdownState` off the composition thread
- Customizable components via `MarkdownComponents`
- Built on JetBrains Markdown parser (0.7.17)
- Apache 2.0 license

## 2. Architecture & Design Principles

### Dependency Addition

Add the M3 variant to the version catalog and the `job-application-details-ui` module:

- **Version catalog** (`gradle/libs.versions.toml`):
  - Version: `markdown-renderer = "0.46.0"`
  - Library: `markdown-renderer-m3 = { module = "com.mikepenz:multiplatform-markdown-renderer-m3", version.ref = "markdown-renderer" }`

- **Module** (`frontend/features/job-application-details/ui/build.gradle.kts`):
  - `commonMain.dependencies { implementation(libs.markdown-renderer-m3) }`

### Rendering Approach

Replace the `Text` composable in `MarkdownPreviewDialog` with the `Markdown` composable from `com.mikepenz.markdown.m3`. For non-trivial content, use `rememberMarkdownState` to parse asynchronously off the composition thread:

```kotlin
val state = rememberMarkdownState(content)
Markdown(state = state)
```

This handles:
- Headers (H1-H6) with proper sizing
- Bold, italic, strikethrough
- Ordered and unordered lists
- Code blocks with syntax highlighting (optional, via `-code` module)
- Links (rendered as clickable spans)
- Tables (GFM)
- Blockquotes
- Horizontal rules

### Styling

The library provides `markdownColor()` and `markdownTypography()` for theming. The renderer will inherit the app's Material 3 theme by default, ensuring consistency with the rest of the UI. No custom component overrides are needed initially — the default M3 styling is sufficient for artifact preview.

### Scope

This change is confined to `MarkdownPreviewDialog` in `DetailDialogs.kt`. No changes to the artifact model, data layer, or sync script are needed — those were already fixed in the previous commit.

## 3. Proposed Changes & Implementation Steps

### [MODIFY] `gradle/libs.versions.toml`
- Add version: `markdown-renderer = "0.46.0"`
- Add library: `markdown-renderer-m3 = { module = "com.mikepenz:multiplatform-markdown-renderer-m3", version.ref = "markdown-renderer" }`

### [MODIFY] `frontend/features/job-application-details/ui/build.gradle.kts`
- Add to `commonMain.dependencies`: `implementation(libs.markdown-renderer-m3)`

### [MODIFY] `frontend/features/job-application-details/ui/.../DetailDialogs.kt`
- Replace the `Text` composable (lines 432-436) that displays raw content with:
  ```kotlin
  val state = rememberMarkdownState(content!!)
  Markdown(
      state = state,
      modifier = Modifier
          .fillMaxWidth()
          .padding(MaterialTheme.spacing.subMedium)
  )
  ```
- Handle the loading state during async parsing (the library provides a loading indicator)
- Keep the error state handling for `loadError`

### Verification
- Compile `:frontend:features-job-application-details-ui:compileKotlinJvm`
- Verify on desktop that markdown artifacts (e.g., `outcome.md`) render with proper formatting
- Verify on Android that inline content renders as formatted markdown

## 4. Verification Plan & Testing Checklist

- [ ] `./gradlew :frontend:features-job-application-details-ui:compileKotlinJvm` — BUILD SUCCESSFUL
- [ ] Desktop app: Open an artifact with markdown content and verify headers, lists, bold, code blocks render correctly
- [ ] Android app: Open an artifact with inline content and verify markdown renders (not raw text)
- [ ] Edge case: Empty content — should not crash
- [ ] Edge case: Very long content — should scroll properly
- [ ] Edge case: Content with links — links should be clickable

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/2026-11-10/190100_markdown_rendering_in_artifacts_preview.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
