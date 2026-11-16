package dev.fnc314.aijobtracker.frontend.app.core

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point creating the root [UIViewController] hosting the Compose Multiplatform [App].
 */
public fun MainViewController(): UIViewController = ComposeUIViewController { App() }