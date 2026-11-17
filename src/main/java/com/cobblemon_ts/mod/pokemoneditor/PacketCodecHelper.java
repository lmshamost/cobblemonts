package com.cobblemon_ts.mod.pokemoneditor;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.Function;

// [线性-HELPER] 为 Yarn 1.21 提供 composite/map 方法，因为 Yarn 映射中 PacketCodec 没有这些静态方法
public class PacketCodecHelper {

    // ---- composite (1-6 参数) ----
    public static <B extends ByteBuf, V1, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1, Function<V1, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); }
        };
    }

    public static <B extends ByteBuf, V1, V2, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1,
                                 PacketCodec<B, V2> c2, Function<O, V2> g2,
                                  F2<V1, V2, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf), c2.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); c2.encode(buf, g2.apply(obj)); }
        };
    }

    public static <B extends ByteBuf, V1, V2, V3, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1,
                                 PacketCodec<B, V2> c2, Function<O, V2> g2,
                                  PacketCodec<B, V3> c3, Function<O, V3> g3,
                                  F3<V1, V2, V3, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); c2.encode(buf, g2.apply(obj)); c3.encode(buf, g3.apply(obj)); }
        };
    }

    public static <B extends ByteBuf, V1, V2, V3, V4, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1,
                                 PacketCodec<B, V2> c2, Function<O, V2> g2,
                                  PacketCodec<B, V3> c3, Function<O, V3> g3,
                                  PacketCodec<B, V4> c4, Function<O, V4> g4,
                                  F4<V1, V2, V3, V4, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); c2.encode(buf, g2.apply(obj)); c3.encode(buf, g3.apply(obj)); c4.encode(buf, g4.apply(obj)); }
        };
    }

    public static <B extends ByteBuf, V1, V2, V3, V4, V5, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1,
                                 PacketCodec<B, V2> c2, Function<O, V2> g2,
                                  PacketCodec<B, V3> c3, Function<O, V3> g3,
                                  PacketCodec<B, V4> c4, Function<O, V4> g4,
                                  PacketCodec<B, V5> c5, Function<O, V5> g5,
                                  F5<V1, V2, V3, V4, V5, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf), c5.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); c2.encode(buf, g2.apply(obj)); c3.encode(buf, g3.apply(obj)); c4.encode(buf, g4.apply(obj)); c5.encode(buf, g5.apply(obj)); }
        };
    }

    public static <B extends ByteBuf, V1, V2, V3, V4, V5, V6, O>
    PacketCodec<B, O> composite(PacketCodec<B, V1> c1, Function<O, V1> g1,
                                 PacketCodec<B, V2> c2, Function<O, V2> g2,
                                  PacketCodec<B, V3> c3, Function<O, V3> g3,
                                  PacketCodec<B, V4> c4, Function<O, V4> g4,
                                  PacketCodec<B, V5> c5, Function<O, V5> g5,
                                  PacketCodec<B, V6> c6, Function<O, V6> g6,
                                  F6<V1, V2, V3, V4, V5, V6, O> constructor) {
        return new PacketCodec<>() {
            public O decode(B buf) { return constructor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf), c5.decode(buf), c6.decode(buf)); }
            public void encode(B buf, O obj) { c1.encode(buf, g1.apply(obj)); c2.encode(buf, g2.apply(obj)); c3.encode(buf, g3.apply(obj)); c4.encode(buf, g4.apply(obj)); c5.encode(buf, g5.apply(obj)); c6.encode(buf, g6.apply(obj)); }
        };
    }

    // ---- mapped (替代 .map()) ----
    public static <B extends ByteBuf, T, R>
    PacketCodec<B, R> mapped(PacketCodec<B, T> base, Function<T, R> from, Function<R, T> to) {
        return new PacketCodec<>() {
            public R decode(B buf) { return from.apply(base.decode(buf)); }
            public void encode(B buf, R obj) { base.encode(buf, to.apply(obj)); }
        };
    }

    // ---- 函数接口 ----
    @FunctionalInterface public interface F2<A, B, R> { R apply(A a, B b); }
    @FunctionalInterface public interface F3<A, B, C, R> { R apply(A a, B b, C c); }
    @FunctionalInterface public interface F4<A, B, C, D, R> { R apply(A a, B b, C c, D d); }
    @FunctionalInterface public interface F5<A, B, C, D, E, R> { R apply(A a, B b, C c, D d, E e); }
    @FunctionalInterface public interface F6<A, B, C, D, E, F, R> { R apply(A a, B b, C c, D d, E e, F f); }
}