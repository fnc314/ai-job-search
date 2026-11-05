package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Parses namespaced Firestore strings into Kotlin enums by delegating to each enum's `fromFirestore`.
 * Maintained for backward compatibility.
 */
public object EnumParsing {

    public fun parseTrackerStatus(raw: String?): TrackerStatus =
        TrackerStatus.fromFirestore(raw)

    public fun parseApplicationChannel(raw: String?): ApplicationChannel =
        ApplicationChannel.fromFirestore(raw)

    public fun parseTrackerEventType(raw: String?): TrackerEventType =
        TrackerEventType.fromFirestore(raw)

    public fun parseTrackerArtifactKind(raw: String?): TrackerArtifactKind =
        TrackerArtifactKind.fromFirestore(raw)

    public fun parseWorkMode(raw: String?): WorkMode =
        WorkMode.fromFirestore(raw)

    public fun parseEventActor(raw: String?): EventActor =
        EventActor.fromFirestore(raw)

    public fun parseConfidence(raw: String?): Confidence =
        Confidence.fromFirestore(raw)

    public fun parseDerivedFrom(raw: String?): DerivedFrom =
        DerivedFrom.fromFirestore(raw)

    public fun parseTrackerEditState(raw: String?): TrackerEditState =
        TrackerEditState.fromFirestore(raw)

    public fun parseTrackerEditKind(raw: String?): TrackerEditKind =
        TrackerEditKind.fromFirestore(raw)

    public fun parseTrigger(raw: String?): Trigger =
        Trigger.fromFirestore(raw)
}
