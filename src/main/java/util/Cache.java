package util;

import java.util.function.Supplier;

public class Cache<T> {
    private T t;
    private Supplier<T> supplier;
    public Cache(Supplier<T> supplier) {
        this.supplier  = supplier;
    }

    public T get() {
        if(t==null) {
            t = supplier.get();
        }
        return t;
    }

}
