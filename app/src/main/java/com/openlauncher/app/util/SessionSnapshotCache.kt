package com.openlauncher.app.util

/** Reuse an unchanged session value even when Android unparcels a new Bitmap. */
class SessionSnapshotCache<K, S, V> {
    private val values = mutableMapOf<K, Pair<S, V>>()
    fun value(key: K, signature: S, force: Boolean = false, create: () -> V): V {
        val previous = values[key]
        if (!force && previous?.first == signature) return previous.second
        return create().also { values[key] = signature to it }
    }
    fun retain(keys: Set<K>) { values.keys.retainAll(keys) }
    fun clear() { values.clear() }
}
