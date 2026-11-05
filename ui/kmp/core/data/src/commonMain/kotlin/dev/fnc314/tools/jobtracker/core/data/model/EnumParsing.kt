package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Centralized parser dispatching Firestore document string representations to typed enum instances.
 *
 * Provides a unified entry point delegating to each domain enum's respective `fromFirestore` factory method.
 */
public object EnumParsing {

    /**
     * Parses a status string into a [TrackerStatus] constant.
     *
     * @param raw Raw status text from Firestore.
     * @return The parsed [TrackerStatus] or [TrackerStatus.UNSPECIFIED].
     */
    public fun parseTrackerStatus(raw: String?): TrackerStatus =
        TrackerStatus.fromFirestore(raw)

    /**
     * Parses a channel string into an [ApplicationChannel] constant.
     *
     * @param raw Raw channel text from Firestore.
     * @return The parsed [ApplicationChannel] or [ApplicationChannel.UNSPECIFIED].
     */
    public fun parseApplicationChannel(raw: String?): ApplicationChannel =
        ApplicationChannel.fromFirestore(raw)

    /**
     * Parses an event type string into a [TrackerEventType] constant.
     *
     * @param raw Raw event string from Firestore.
     * @return The parsed [TrackerEventType] or [TrackerEventType.UNSPECIFIED].
     */
    public fun parseTrackerEventType(raw: String?): TrackerEventType =
        TrackerEventType.fromFirestore(raw)

    /**
     * Parses an artifact kind string into a [TrackerArtifactKind] constant.
     *
     * @param raw Raw artifact kind string from Firestore.
     * @return The parsed [TrackerArtifactKind] or [TrackerArtifactKind.UNSPECIFIED].
     */
    public fun parseTrackerArtifactKind(raw: String?): TrackerArtifactKind =
        TrackerArtifactKind.fromFirestore(raw)

    /**
     * Parses a work arrangement string into a [WorkMode] constant.
     *
     * @param raw Raw work mode string from Firestore.
     * @return The parsed [WorkMode] or [WorkMode.UNSPECIFIED].
     */
    public fun parseWorkMode(raw: String?): WorkMode =
        WorkMode.fromFirestore(raw)

    /**
     * Parses an event actor string into an [EventActor] constant.
     *
     * @param raw Raw actor string from Firestore.
     * @return The parsed [EventActor] or [EventActor.UNSPECIFIED].
     */
    public fun parseEventActor(raw: String?): EventActor =
        EventActor.fromFirestore(raw)

    /**
     * Parses a confidence string into a [Confidence] constant.
     *
     * @param raw Raw confidence string from Firestore.
     * @return The parsed [Confidence] or [Confidence.UNSPECIFIED].
     */
    public fun parseConfidence(raw: String?): Confidence =
        Confidence.fromFirestore(raw)

    /**
     * Parses a derivation origin string into a [DerivedFrom] constant.
     *
     * @param raw Raw derivation string from Firestore.
     * @return The parsed [DerivedFrom] or [DerivedFrom.UNSPECIFIED].
     */
    public fun parseDerivedFrom(raw: String?): DerivedFrom =
        DerivedFrom.fromFirestore(raw)

    /**
     * Parses a proposal state string into a [TrackerEditState] constant.
     *
     * @param raw Raw edit state string from Firestore.
     * @return The parsed [TrackerEditState] or [TrackerEditState.UNSPECIFIED].
     */
    public fun parseTrackerEditState(raw: String?): TrackerEditState =
        TrackerEditState.fromFirestore(raw)

    /**
     * Parses a proposal category string into a [TrackerEditKind] constant.
     *
     * @param raw Raw edit kind string from Firestore.
     * @return The parsed [TrackerEditKind] or [TrackerEditKind.UNSPECIFIED].
     */
    public fun parseTrackerEditKind(raw: String?): TrackerEditKind =
        TrackerEditKind.fromFirestore(raw)

    /**
     * Parses a trigger origin string into a [Trigger] constant.
     *
     * @param raw Raw trigger string from Firestore.
     * @return The parsed [Trigger] or [Trigger.UNSPECIFIED].
     */
    public fun parseTrigger(raw: String?): Trigger =
        Trigger.fromFirestore(raw)
}
