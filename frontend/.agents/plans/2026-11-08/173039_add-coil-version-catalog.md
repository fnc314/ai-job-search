# Implementation Plan: Add Coil Version Catalog Entries for CMP-Compatible Images

**Status:** Completed  
**Date:** 2026-11-08
**Target:** `ui/kmp` (`gradle/libs.versions.toml`)

---

## 1. Executive Summary & Goals

Add Coil 3.6.3 (`io.coil-kt.coil3`) version catalog entries to `gradle/libs.versions.toml` to support Compose Multiplatform (CMP) compatible image loading, including a `coil` bundle for projects consuming multiple Coil artifacts.

## 2. Architecture & Design Principles

- **CMP-Native Image Loading**: Use Coil 3 (`io.coil-kt.coil3`).
- **Version Catalog Bundles**: Define a `coil` bundle for streamlined multi-artifact consumption.

## 3. Proposed Changes & Implementation Steps

### [MODIFY] [libs.versions.toml](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/gradle/libs.versions.toml)
- Add `coil = "3.6.3"`.
- Add `coil-compose`, `coil-core`, `coil-network-ktor3`.
- Add `coil` bundle under `[bundles]`.

## 4. Verification Plan & Testing Checklist

### Automated Tests
- Run Gradle metadata build.
