package com.cobblemon_ts.mod;

/** [线性-REG] 替代NeoForge DeferredItem的简单包装器 */
public class RegistryHelper {
    public static class DeferredItem<T> {
        private final T item;
        public DeferredItem(T item) { this.item = item; }
        public T get() { return item; }
    }
    public static class DeferredBlock<T> {
        private final T block;
        public DeferredBlock(T block) { this.block = block; }
        public T get() { return block; }
    }
}