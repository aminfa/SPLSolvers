package de.upb.spl.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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


    public static class ChainedIterator<I, T> implements Iterator<T> {
        private final Iterator<I> basedOnIterator;

        private final Function<I, Iterator<T>> supplier;

        private Iterator<T> currentIterator;

        public ChainedIterator(Iterator<I> basedOnIterator, Function<I, Iterator<T>> supplier) {
            this.basedOnIterator = Objects.requireNonNull(basedOnIterator);
            this.supplier = Objects.requireNonNull(supplier);
            findNext();
        }

        private void findNext() {
            while ((currentIterator == null || !currentIterator.hasNext()) && basedOnIterator.hasNext()) {
                currentIterator = supplier.apply(basedOnIterator.next());
            }
            if (currentIterator != null && !currentIterator.hasNext()) {
                currentIterator = null;
            }
        }

        @Override
        public boolean hasNext() {
            if (currentIterator == null) {
                return false;
            } else if (currentIterator.hasNext()) {
                return true;
            } else {
                findNext();
                return hasNext();
            }
        }

        @Override
        public T next() {
            return currentIterator.next();
        }
    }
}
