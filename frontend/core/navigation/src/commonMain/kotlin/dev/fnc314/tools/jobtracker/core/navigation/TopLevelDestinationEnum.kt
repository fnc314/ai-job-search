package dev.fnc314.tools.jobtracker.core.navigation

/**
 * Finite enum of top-level destinations for bottom navigation and navigation rail.
 *
 * Replaces the runtime `is JobApplicationListRoute` / `is JobApplicationCompaniesRoute` checks in [AppScaffold]
 * with a type-safe, extensible enum. Since `core/navigation` cannot depend on feature
 * route classes or string resources, this enum is a simple identifier. All UI metadata
 * (icons, labels, routes) is resolved by [AppScaffold] using a mapping function.
 */
public enum class TopLevelDestinationEnum {
    Applications,
    Companies,
    Edits,
    Settings,
}
