package mycontroller;

import java.util.HashMap;

/**
 * Extension of Java's HashMap that behaves like a simple version of Python's DefaultDict.
 * @param <K> Type of the key
 * @param <V> Type of the value
 */
public class DefaultDict<K, V> extends HashMap<K, V> {
    /**
     * Default value returned from the map.
     */
    private V mDefaultValue;

    public DefaultDict(V defaultValue) {
        mDefaultValue = defaultValue;
    }

    /**
     * Get the value corresponding to a key in the map. If The key does not exist then the
     * default value set at object construction is used.
     *
     * @param key Key to get from the map
     * @return Value corresponding to the key, or the default value
     */
    @Override
    public V get(Object key) {
        if (super.containsKey(key)) {
            return super.get(key);
        }
        return mDefaultValue;
    }
}
