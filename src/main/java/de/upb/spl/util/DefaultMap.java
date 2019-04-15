package de.upb.spl.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultMap<K, V> {

	private transient Function<K, V> defaultValueSupplier;
	private final Object2ObjectMap<K, V> innerMap = new Object2ObjectOpenHashMap<>();

	public DefaultMap(Supplier<V> defaultValueSupplier) {
		this(l -> defaultValueSupplier.get());
	}
	public DefaultMap(Function<K, V> defaultValueSupplier) {
		this.defaultValueSupplier = defaultValueSupplier;
	}

	public V get(K key) {
		if (!innerMap.containsKey(key)) {
			innerMap.put(key, getDefaultSupplier().apply(key));
		}
		return innerMap.get(key);
	}

	public void put(K key, V value) {
		innerMap.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
	}

	private Function<K, V> getDefaultSupplier() {
		if (defaultValueSupplier == null) {
			/*
			 * the field is transient thus is null after deserialisation.
			 */
			defaultValueSupplier = k -> null;
		}
		return defaultValueSupplier;
	}

	public void setDefaultFunc(Function<K, V> func) {
	    this.defaultValueSupplier = func;
    }

	public String toString() {
		return innerMap.toString();
	}
}
