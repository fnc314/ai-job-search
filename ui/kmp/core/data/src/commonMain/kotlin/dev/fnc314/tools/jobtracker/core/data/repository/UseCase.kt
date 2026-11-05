package dev.fnc314.tools.jobtracker.core.data.repository

/**
 * Universal functional interface defining single-responsibility domain use cases in clean architecture.
 *
 * @param Params Parameter input type received upon execution.
 * @param Result Resulting return type emitted upon completion.
 */
public interface UseCase<in Params, out Result> {

    /**
     * Executes the business logic encapsulated by this use case asynchronously.
     *
     * @param params Input parameters required for execution.
     * @return The operation result.
     */
    public suspend operator fun invoke(params: Params): Result
}
