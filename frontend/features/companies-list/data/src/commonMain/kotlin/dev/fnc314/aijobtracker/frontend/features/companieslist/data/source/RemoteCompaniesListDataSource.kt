package dev.fnc314.aijobtracker.frontend.features.companieslist.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.Company

/**
 * Remote data source contract querying employer documents from Firestore.
 */
internal interface RemoteCompaniesListDataSource {
    /**
     * Fetches all company records from the remote Firestore `companies` collection.
     */
    public suspend operator fun invoke(): List<Company>
}
