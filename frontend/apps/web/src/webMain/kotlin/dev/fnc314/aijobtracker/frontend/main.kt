package dev.fnc314.aijobtracker.frontend

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.fnc314.aijobtracker.frontend.app.core.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
      App()
    }
}