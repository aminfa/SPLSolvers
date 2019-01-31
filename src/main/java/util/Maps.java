package util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility methods for {@link Map}.
 */
public class Maps {

	/**
	 * Inserts every key value pair from mapFrom to mapTo by using the applying translator function to the values.
	 * @param mapFrom key value pairs are taken from this map.
	 * @param mapTo map filled with new values taken from mapFrom
	 * @param translator function that casts values taken from mapFrom to values with fitting type to be put into mapTo.
	 * @param <T> Key type of both map
	 * @param <U> value type of mapFrom
	 * @param <V> value type of mapTo
	 */
	public static <T,U,V> void translate(Map<T,U> mapFrom, Map<T,V> mapTo, Function<U,V> translator) {
		for(T t : mapFrom.keySet()) {
			U u = mapFrom.get(t);
			V v = translator.apply(u);
			mapTo.put(t, v);
		}
	}

	/**
	 * Returns Intersection of the keysets from a and b, denoted A ∩ B, which is the set of all keys that are key of both A and B.
	 * @param <T> the type of the keys.
	 * @return Intersection of the keysets
	 */
	public static <T> Set<T> keyIntersection(Map<T, ?> a, Map<T, ?> b) {
		Set<T> intersection = new HashSet<>(a.keySet()); // copy the key set of a.
		// remove every key that is also in b:
		intersection.retainAll(b.keySet());
		return intersection;
	}
}
