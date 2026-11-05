package dev.fnc314.tools.jobtracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable

/**
 * Navigation 3 style route keys implementing serializable navigation state.
 */
sealed interface NavRoute

@Serializable
data object SignInRoute : NavRoute

@Serializable
data object ListRoute : NavRoute

@Serializable
data class DetailRoute(val applicationId: String) : NavRoute

@Serializable
data object AddApplicationRoute : NavRoute

@Serializable
data object AddLeadRoute : NavRoute

@Serializable
data object EditsRoute : NavRoute

@Serializable
data object SettingsRoute : NavRoute

/**
 * Manages backstack and navigation actions following Navigation 3 paradigms.
 */
class Navigator(
    val backStack: SnapshotStateList<NavRoute>
) {
    val currentRoute: NavRoute
        get() = backStack.lastOrNull() ?: ListRoute

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigateTo(route: NavRoute) {
        backStack.add(route)
    }

    fun goBack(): Boolean {
        if (canGoBack) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }

    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}

@Composable
fun rememberNavigator(startRoute: NavRoute = ListRoute): Navigator {
    val backStack = remember {
        mutableStateListOf(startRoute)
    }
    return remember(backStack) {
        Navigator(backStack)
    }
}

/**
 * Entry provider representing Navigation 3 NavEntry definitions.
 */
class NavEntryScope(val route: NavRoute) {
    val key: Any get() = route
}

class NavEntryProviderScope {
    @PublishedApi
    internal val entries = mutableMapOf<kotlin.reflect.KClass<*>, @Composable (NavRoute) -> Unit>()

    inline fun <reified T : NavRoute> entry(noinline content: @Composable (T) -> Unit) {
        entries[T::class] = { route ->
            @Suppress("UNCHECKED_CAST")
            content(route as T)
        }
    }

    @Composable
    fun ContentFor(route: NavRoute) {
        val entry = entries[route::class]
        if (entry != null) {
            entry(route)
        } else {
            error("No entry found for route: $route")
        }
    }
}

fun navEntryProvider(block: NavEntryProviderScope.() -> Unit): NavEntryProviderScope {
    return NavEntryProviderScope().apply(block)
}

/**
 * NavDisplay composable that handles back stack rendering and back events according to Navigation 3 principles.
 */
@Composable
fun NavDisplay(
    navigator: Navigator,
    entryProvider: NavEntryProviderScope,
    onBack: () -> Unit = { navigator.goBack() }
) {
    BackHandler(enabled = navigator.canGoBack) {
        onBack()
    }

    val currentRoute = navigator.currentRoute
    entryProvider.ContentFor(currentRoute)
}
