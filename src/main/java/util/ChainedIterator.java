package util;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public class ChainedIterator<I, T> implements Iterator<T> {
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
