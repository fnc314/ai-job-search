package dev.fnc314.tools.jobtracker.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data object SignInRoute

@Serializable
internal data object ListRoute

@Serializable
internal data class DetailRoute(val applicationId: String)

@Serializable
internal data object AddApplicationRoute

@Serializable
internal data object AddLeadRoute

@Serializable
internal data object EditsRoute

@Serializable
internal data object SettingsRoute
