package net.zoda.housing.utils;

import lombok.Getter;
import lombok.Setter;

public final class Pair<A, B> {

    @Setter
    @Getter
    private A a;
    @Setter
    @Getter
    private B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public Pair() {
        this(null, null);
    }
}
