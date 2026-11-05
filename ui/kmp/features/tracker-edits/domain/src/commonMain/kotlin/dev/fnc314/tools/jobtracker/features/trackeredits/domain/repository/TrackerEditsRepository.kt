package dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

/**
 * Domain repository contract managing staging, review, approval, and rejection of edit proposals.
 */
public interface TrackerEditsRepository {

    /**
     * Retrieves all edit proposals currently in [dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState.PENDING] review status.
     *
     * @return List of pending [TrackerEdit] proposals.
     */
    public suspend fun getPendingEdits(): List<TrackerEdit>

    /**
     * Approves a pending proposal, applying mutations to the target document and transitioning state to APPROVED.
     *
     * @param id The proposal identifier.
     * @param note Optional approval rationale or note.
     * @return The updated [TrackerEdit] instance.
     */
    public suspend fun approveEdit(id: String, note: String?): TrackerEdit

    /**
     * Rejects a pending proposal without modifying the underlying application.
     *
     * @param id The proposal identifier.
     * @param reason Explanation for rejection.
     * @return The updated [TrackerEdit] instance in REJECTED state.
     */
    public suspend fun rejectEdit(id: String, reason: String): TrackerEdit

    /**
     * Scans for pending proposals that have exceeded their expiration deadline and marks them EXPIRED.
     *
     * @return Count of proposals transitioned to EXPIRED.
     */
    public suspend fun expireStaleEdits(): Int
}
