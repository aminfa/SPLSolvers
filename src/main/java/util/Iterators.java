package util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

public class Iterators {
	public static <T> List<T> TO_LIST(Iterator<T> iterator) {
		List<T> list = new ArrayList<>();
		iterator.forEachRemaining(list::add);
		return list;
	}

	public static String ordinal(int i) {
		String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
		switch (i % 100) {
			case 11:
			case 12:
			case 13:
				return i + "th";
			default:
				return i + sufixes[i % 10];

		}
	}
	public static <T> Iterator<Integer> map(final Iterator<T> iterator, final Function<T, Integer> function) {
		return new Iterator<Integer>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Integer next() {
				return function.apply(iterator.next());
			}
		};
	}


}
