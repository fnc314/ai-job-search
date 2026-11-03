package dev.fnc314.tools.jobtracker.domain

interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}
