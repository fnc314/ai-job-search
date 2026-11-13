package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Common wire contract implemented by canonical domain enums and sealed interface variants.
 *
 * Exposes the string wire key used for Firestore persistence and Room storage,
 * the human-readable display label, and an optional integer wire tag for Protobuf interoperability.
 */
public interface WireContract {
    /**
     * Canonical namespaced or un-namespaced string representation stored in Firestore and Room.
     */
    public val wireKey: String

    /**
     * Human-readable display label formatted for presentation in UI badges, pickers, and cards.
     */
    public val displayName: String

    /**
     * Optional integer tag matching Protobuf wire number contracts, or -1 if unassigned.
     */
    public val wireTag: Int
        get() = -1
}

/**
 * Builds an immutable O(1) lookup map indexing entries by their canonical [WireContract.wireKey]
 * and enum [Enum.name].
 */
public fun <E> Iterable<E>.toWireLookupMap(): Map<String, E>
    where E : Enum<E>, E : WireContract = buildMap {
        for (entry in this@toWireLookupMap) {
            put(entry.wireKey.lowercase(), entry)
            put(entry.name.lowercase(), entry)
        }
    }

/**
 * Builds an immutable O(1) lookup map indexing entries by canonical [WireContract.wireKey],
 * enum [Enum.name], and additional custom aliases provided by [aliasSelector].
 */
public inline fun <E> Iterable<E>.toWireLookupMap(
    crossinline aliasSelector: (E) -> List<String>
): Map<String, E> where E : Enum<E>, E : WireContract = buildMap {
    for (entry in this@toWireLookupMap) {
        put(entry.wireKey.lowercase(), entry)
        put(entry.name.lowercase(), entry)
        for (alias in aliasSelector(entry)) {
            put(alias.lowercase(), entry)
        }
    }
}
