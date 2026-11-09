package dev.fnc314.tools.jobtracker.core.data.source

/**
 * Result abstraction representing the completion status of a remote data mutation or network write operation.
 */
public sealed interface DataResult {
    /** Indicates the operation completed successfully without errors. */
    public data object Success : DataResult

    /**
     * Indicates the operation encountered a failure.
     *
     * @property error The underlying [Throwable] describing the error.
     */
    public data class Failure(public val error: Throwable) : DataResult
}
