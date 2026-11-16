# Build Tooling & Gradle Convention Plugins

The `gradle/tools/project-plugins` included build provides shared Gradle convention plugins and architectural build logic across the repository.

## Overview

- **`KotlinMultiplatformConventionPlugin` (`dev.fnc314.aijobtracker.kotlin-multiplatform`)**: Standardizes KMP compilation across JVM, JS, WasmJS, and iOS targets, enabling explicit API mode and wiring Detekt/Dokka.
- **`AndroidKmpLibraryConventionPlugin` (`dev.fnc314.aijobtracker.android-kmp-library`)**: Configures Android compilation targets (`compileSdk`, `minSdk`, namespace calculation) for KMP modules.
- **`ComposeConventionPlugin` (`dev.fnc314.aijobtracker.compose`)**: Applies Compose Multiplatform and Jetpack Compose compiler conventions.
- **`DokkaConventionPlugin` (`dev.fnc314.aijobtracker.dokka`)**: Standardizes Dokka 2.3.0 documentation generation, binds local `README.md` frontpieces to publications and source sets, and registers modules for hierarchical site navigation.
- **`DetektConventionPlugin` (`dev.fnc314.aijobtracker.detekt`)**: Standardizes code quality analysis and architectural rule enforcement.
