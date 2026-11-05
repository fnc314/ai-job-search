package dev.fnc314.tools.jobtracker

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

public fun MainViewController(): UIViewController = ComposeUIViewController { App() }