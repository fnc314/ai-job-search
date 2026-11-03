package dev.fnc314.tools.jobtracker

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FNC314JobSearchTracker",
    ) {
        App()
    }
}