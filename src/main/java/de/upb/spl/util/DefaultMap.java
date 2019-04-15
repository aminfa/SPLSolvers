package de.upb.spl.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultMap<K, V> {

	private transient BiFunction<DefaultMap<K, V> , K, V> incaseMissing;
	private final Object2ObjectMap<K, V> innerMap = new Object2ObjectOpenHashMap<>();

	public DefaultMap(Supplier<V> defaultValueSupplier) {
	    this(l -> defaultValueSupplier.get());
	}


    public DefaultMap(Function<K, V> defaultValueSupplier) {
        this((map, l) -> defaultValueSupplier.apply(l));
    }

    public DefaultMap(BiFunction<DefaultMap<K, V> , K, V> defaultValueSupplier) {
        this.incaseMissing = defaultValueSupplier;
    }

    public V get(K key) {
		if (!innerMap.containsKey(key)) {
			innerMap.put(key, getDefaultSupplier().apply(this, key));
		}
		return innerMap.get(key);
	}

	public void put(K key, V value) {
		innerMap.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
	}

	private BiFunction<DefaultMap<K, V> , K, V> getDefaultSupplier() {
		if (incaseMissing == null) {
			/*
			 * the field is transient thus is null after deserialisation.
			 */
            incaseMissing = (map, k) -> null;
		}
		return incaseMissing;
	}

	public String toString() {
		return innerMap.toString();
	}
}
