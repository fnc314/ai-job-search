package dev.fnc314.tools.jobtracker.core.data.repository

public interface UseCase<in Params, out Result> {
    public suspend operator fun invoke(params: Params): Result
}
