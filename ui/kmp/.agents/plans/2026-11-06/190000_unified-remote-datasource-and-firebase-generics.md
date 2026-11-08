# Architecture Plan: Unified RemoteDataSource and Feature-specific Firebase Generics

**Status:** Draft / Proposed  
**Date:** 2026-11-06  
**Target:** `ui/kmp`  

---

## 1. Executive Summary & Goals

As the `ui/kmp` workspace expands across multiple features (`job-applications-list`, `job-application-details`, `add-job-application`, `tracker-edits`, `settings`, `sign-in`), remote data sources interacting with Firebase Firestore currently duplicate collection lookup logic, error mapping, and document snapshot deserialization. 

This architecture plan outlines the design and refactoring strategy to introduce a reusable, generic base remote data source (`BaseFirebaseRemoteDataSource` or unified remote abstraction) that standardizes CRUD operations, flow collection, error handling, and type-safe document mapping across all feature-specific data modules.

---

## 2. Architecture & Design Principles

- **DRY Principle for Firebase Operations**: Centralize common Firestore query builders, document references, batch operations, and snapshot listeners into a shared core or feature utility.
- **Type Safety & Generics**: Leverage Kotlin reified type parameters and kotlinx.serialization / Firestore serialization mappings to convert documents to domain entities cleanly.
- **Clean Architecture Boundary**: Keep data source implementations strictly inside their respective feature data modules (`features/*/data`) while sharing underlying infrastructure abstractions.
- **Coroutines & Flows**: Maintain asynchronous non-blocking data streams using `kotlinx.coroutines.flow`.

---

## 3. Proposed Changes & Implementation Steps

### [NEW / MODIFY] Core / Feature Data Source Abstractions
- Define shared Firestore extension helpers for common document collection queries.
- Refactor feature data sources (`RemoteJobApplicationsListDataSourceImpl`, `RemoteJobApplicationsDetailsDataSourceImpl`, etc.) to inherit or compose generic Firestore operations.

---

## 4. Verification Plan & Testing Checklist

- [ ] Compile all KMP modules via `./gradlew compileCommonMainKotlinMetadata`.
- [ ] Run unit tests for all features.
- [ ] Verify zero regression in data synchronization and UI responsiveness.
