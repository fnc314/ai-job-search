# Implementation Plan: Rework Sign-In Feature Architecture & Resolve Koin Crash

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** `ui/kmp/features/sign-in`  
**Depends On:** `052100_fix-settings-customization-and-kmp-inspector-plan.md`  

---

## 1. Problem Statement
On a fresh installation, the user begins in an unauthenticated state (`isAuthenticated == false`), which directs navigation to `SignInRoute`. Launching `SignInScreen` triggers Koin resolution for `SignInViewModel`, which immediately crashes with:
```
org.koin.core.error.NoDefinitionFoundException: No definition found for type 'dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase' on scope '['_root_']'.. Check or add definition for type 'dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase' in scope '_root_'.
```
Investigation reveals:
1. `SignInDomainModule` only registers `factoryOf(::SignInUseCaseImpl) bind SignInUseCase::class`.
2. `SignOutUseCase`, `ObserveAuthStateUseCase`, and `GetCurrentUserUseCase` are defined together in a single file `AuthUseCases.kt` as concrete classes without interface abstraction or registration in `signInDomainModule`.
3. `SignInViewModel` expects `SignOutUseCase` and `ObserveAuthStateUseCase` in its constructor, which fails to resolve in Koin.
4. The feature structure violates the workspace conventions where each use case interface is in its own file, each implementation is in an `impl` package, visibility is declared with public interfaces and internal implementations, and `features:sign-in:domain` depends on `features:sign-in:data` while `features:sign-in:ui` depends on `features:sign-in:domain`.

---

## 2. Proposed Changes

### 2.1 `features:sign-in:domain`
- **[DELETE]** [`AuthUseCases.kt`](../../features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/AuthUseCases.kt).
- **[NEW]** `SignOutUseCase.kt`: Public interface declaring `suspend operator fun invoke(): Result<Unit>`.
- **[NEW]** `impl/SignOutUseCaseImpl.kt`: Internal class implementing `SignOutUseCase` backed by `AuthRepository.signOut()`.
- **[NEW]** `ObserveAuthStateUseCase.kt`: Public interface declaring `operator fun invoke(): StateFlow<AuthState>`.
- **[NEW]** `impl/ObserveAuthStateUseCaseImpl.kt`: Internal class implementing `ObserveAuthStateUseCase` backed by `AuthRepository.authState`.
- **[NEW]** `GetCurrentUserUseCase.kt`: Public interface declaring `operator fun invoke(): UserAccount?`.
- **[NEW]** `impl/GetCurrentUserUseCaseImpl.kt`: Internal class implementing `GetCurrentUserUseCase` backed by `AuthRepository.currentUser`.
- **[MODIFY]** `SignInUseCase.kt` & `impl/SignInUseCaseImpl.kt`: Ensure proper KDoc and visibility modifiers.
- **[MODIFY]** [`SignInDomainModule.kt`](../../features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/di/SignInDomainModule.kt):
  Register all use case implementations:
  ```kotlin
  public val signInDomainModule: Module = module {
      factoryOf(::SignInUseCaseImpl) bind SignInUseCase::class
      factoryOf(::SignOutUseCaseImpl) bind SignOutUseCase::class
      factoryOf(::ObserveAuthStateUseCaseImpl) bind ObserveAuthStateUseCase::class
      factoryOf(::GetCurrentUserUseCaseImpl) bind GetCurrentUserUseCase::class
  }
  ```
- **[MODIFY]** [`AuthUseCasesTest.kt`](../../features/sign-in/domain/src/commonTest/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/AuthUseCasesTest.kt): Update tests to verify interface implementations.

### 2.2 `features:sign-in:ui`
- **[MODIFY]** [`build.gradle.kts`](../../features/sign-in/ui/build.gradle.kts): Remove transitive `api(projects.featuresSignInData)` to enforce strict dependency on `featuresSignInDomain`.
- **[NEW]** `SignInDiTest.kt`: Unit test verifying that `signInFeatureModule` along with mock core dependencies resolves all definitions (including `SignInViewModel`) cleanly.

---

## 3. Verification Plan

### Automated Tests
1. **Unit Tests**:
   - `./ui/kmp/gradlew -p ui/kmp :features-sign-in-domain:jvmTest :features-sign-in-ui:jvmTest`
2. **Architecture & Consistency Tests**:
   - `mise run dev:konsist`
   - `./ui/kmp/gradlew -p ui/kmp detekt`
3. **Android Assembly**:
   - `./ui/kmp/gradlew -p ui/kmp :apps-android:assembleDebug`

### Manual Verification
- Fresh install launches into `SignInRoute` and instantiates `SignInViewModel` without Koin resolution crashes.
