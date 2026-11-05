package dev.fnc314.tools.jobtracker

internal class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}