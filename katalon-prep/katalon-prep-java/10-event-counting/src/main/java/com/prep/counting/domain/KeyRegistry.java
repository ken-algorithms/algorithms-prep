package com.prep.counting.domain;

import java.util.Set;

/**
 * The set of valid keys, held in memory. A few thousand keys is a {@code HashSet}; it is swapped whole on
 * reload (every minute), so readers never lock. Ten million keys would need a Bloom filter in front of a store.
 */
public final class KeyRegistry {

    private volatile Set<String> keys;

    public KeyRegistry(Set<String> keys) {
        this.keys = Set.copyOf(keys);
    }

    public boolean contains(String key) {
        return keys.contains(key);
    }

    public void replace(Set<String> newKeys) {
        this.keys = Set.copyOf(newKeys);
    }

    public int size() {
        return keys.size();
    }
}
