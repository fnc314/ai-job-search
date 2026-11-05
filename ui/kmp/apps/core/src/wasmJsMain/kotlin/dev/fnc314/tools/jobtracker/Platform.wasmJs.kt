package dev.fnc314.tools.jobtracker

internal class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

internal actual fun getPlatform(): Platform = WasmPlatform()