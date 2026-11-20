package dev.fnc314.aijobtracker.frontend.core.navigation

/**
 * Finite enum of top-level destinations for bottom navigation and navigation rail.
 *
 * Mapped to and from canonical proto definition in `dev.fnc314.aijobtracker.records.TopLevelDestination`.
 */
public enum class TopLevelDestination {
    Applications,
    Companies,
    Apply,
    Edits,
    Settings;

    public companion object {
        /**
         * Resolves the UI destination enum from the shared proto enum name or string key.
         */
        public fun fromProtoName(name: String?): TopLevelDestination? = when (name?.uppercase()) {
            "TOP_LEVEL_DESTINATION_APPLICATIONS", "APPLICATIONS" -> Applications
            "TOP_LEVEL_DESTINATION_COMPANIES", "COMPANIES" -> Companies
            "TOP_LEVEL_DESTINATION_APPLY", "APPLY" -> Apply
            "TOP_LEVEL_DESTINATION_EDITS", "EDITS" -> Edits
            "TOP_LEVEL_DESTINATION_SETTINGS", "SETTINGS" -> Settings
            else -> null
        }
    }
}
