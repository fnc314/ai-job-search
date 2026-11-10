package dev.fnc314.tools.jobtracker.features.companieslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Remote data source contract querying employer documents from Firestore.
 */
internal interface RemoteCompaniesListDataSource {
    /**
     * Fetches all company records from the remote Firestore `companies` collection.
     */
    public suspend operator fun invoke(): List<Company>
}
