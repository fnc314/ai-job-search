package dev.fnc314.tools.jobtracker.core.data.source

public sealed interface DataResult {
    public data object Success : DataResult
    public data class Failure(public val error: Throwable) : DataResult
}
