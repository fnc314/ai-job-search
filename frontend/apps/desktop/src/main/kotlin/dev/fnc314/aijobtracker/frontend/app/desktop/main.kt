package dev.fnc314.aijobtracker.frontend.app.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fnc314.aijobtracker.frontend.app.core.App

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "AI Job Tracker",
        ) {
            App()
        }
    }
