package dev.fnc314.tools.jobtracker.core.data.settings

import kotlinx.serialization.Serializable

@Serializable
public enum class UiMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
public data class AppSettings(
    public val showDate: Boolean = true,
    public val showCompany: Boolean = true,
    public val showStatus: Boolean = true,
    public val showFit: Boolean = true,
    public val showRole: Boolean = true,
    public val uiMode: UiMode = UiMode.SYSTEM,
    public val dynamicColor: Boolean = false,
    public val seedColorHex: String = "#2F5597",
    public val renderMarkdownInternally: Boolean = false,
) {
    public fun isDark(systemInDarkTheme: Boolean): Boolean = when (uiMode) {
        UiMode.SYSTEM -> systemInDarkTheme
        UiMode.LIGHT -> false
        UiMode.DARK -> true
    }

    public val darkTheme: Boolean
        get() = uiMode == UiMode.DARK
}
