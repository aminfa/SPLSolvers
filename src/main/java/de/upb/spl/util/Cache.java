package de.upb.spl.util;

import de.upb.spl.benchmarks.BenchmarkEntry;

import java.util.List;
import java.util.function.Supplier;

public class Cache<T> {
    private T t;
    private Supplier<T> supplier;
    public Cache(Supplier<T> supplier) {
        this.supplier  = supplier;
    }

    public static <T> Cache<T> of(T constant) {
        Cache<T> cache = new Cache<>(null);
        cache.t = constant;
        return cache;
    }

    public T get() {
        if(t==null) {
            t = supplier.get();
            supplier = null; // no need for the supplier anymore.
        }
        return t;
    }

}
