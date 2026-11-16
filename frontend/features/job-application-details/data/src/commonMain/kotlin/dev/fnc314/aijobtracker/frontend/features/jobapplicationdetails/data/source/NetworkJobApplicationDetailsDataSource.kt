package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.data.source

import dev.fnc314.aijobtracker.frontend.core.data.source.NetworkDataSource

internal fun interface NetworkJobApplicationDetailsDataSource {
    suspend operator fun invoke(path: String): String
}
