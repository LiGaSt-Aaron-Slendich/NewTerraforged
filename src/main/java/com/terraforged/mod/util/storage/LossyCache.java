package com.terraforged.mod.util.storage;

import com.terraforged.mod.Environment;
import com.terraforged.mod.util.storage.LockUtil;
import com.terraforged.mod.util.storage.LongCache;
import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.util.Mth;

public class LossyCache<T>
implements LongCache<T> {
    protected final long[] keys;
    protected final T[] values;
    protected final int mask;
    protected final Consumer<T> removalListener;

    private LossyCache(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
        capacity = Mth.smallestEncompassingPowerOfTwo((int)capacity);
        this.mask = capacity - 1;
        this.keys = new long[capacity];
        this.values = constructor.apply(capacity);
        this.removalListener = removalListener;
        Arrays.fill(this.keys, Long.MIN_VALUE);
    }

    @Override
    public T computeIfAbsent(int seed, long key, LongCache.SeededKeyFunction<T> function) {
        int hash = LossyCache.hash(key);
        int index = hash & this.mask;
        T value = this.values[index];
        if (this.keys[index] == key && value != null) {
            return value;
        }
        T newValue = function.apply(seed, key);
        this.keys[index] = key;
        this.values[index] = newValue;
        this.onRemove(value);
        return newValue;
    }

    protected void onRemove(T value) {
        if (value != null) {
            this.removalListener.accept(value);
        }
    }

    protected static int hash(long l) {
        return (int)HashCommon.mix((long)l);
    }

    public static <T> LongCache<T> of(int capacity, IntFunction<T[]> constructor) {
        return LossyCache.of(capacity, constructor, t -> {});
    }

    public static <T> LongCache<T> of(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
        return new LossyCache<T>(capacity, constructor, removalListener);
    }

    public static <T> LongCache<T> concurrent(int capacity, IntFunction<T[]> constructor) {
        return LossyCache.concurrent(capacity, constructor, t -> {});
    }

    public static <T> LongCache<T> concurrent(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
        return LossyCache.concurrent(capacity, Environment.CORES, constructor, removalListener);
    }

    public static <T> LongCache<T> concurrent(int capacity, int concurrency, IntFunction<T[]> constructor, Consumer<T> removalListener) {
        return new Concurrent<T>(capacity, concurrency, constructor, removalListener);
    }

    public static class Concurrent<T>
    implements LongCache<T> {
        protected static final int HASH_BITS = Integer.MAX_VALUE;
        protected final int mask;
        protected final Stamped<T>[] buckets;

        public Concurrent(int capacity, int concurrency, IntFunction<T[]> constructor, Consumer<T> removalListener) {
            concurrency = Mth.smallestEncompassingPowerOfTwo((int)concurrency);
            capacity = NoiseUtil.floor((float)capacity / (float)concurrency);
            this.mask = concurrency - 1;
            this.buckets = new Stamped[concurrency];
            for (int i = 0; i < concurrency; ++i) {
                this.buckets[i] = new Stamped<T>(capacity, constructor, removalListener);
            }
        }

        @Override
        public T computeIfAbsent(int seed, long key, LongCache.SeededKeyFunction<T> function) {
            return this.buckets[this.index(key)].computeIfAbsent(seed, key, function);
        }

        protected int index(long key) {
            return Concurrent.spread(key) & this.mask;
        }

        protected static int spread(long h) {
            return (int)(h ^ h >>> 16) & Integer.MAX_VALUE;
        }
    }

    public static class Stamped<T>
    extends LossyCache<T> {
        protected final StampedLock lock = new StampedLock();

        public Stamped(int capacity, IntFunction<T[]> constructor, Consumer<T> removalListener) {
            super(capacity, constructor, removalListener);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public T computeIfAbsent(int seed, long key, LongCache.SeededKeyFunction<T> function) {
            int index = Stamped.hash(key) & this.mask;
            long readStamp = this.lock.tryOptimisticRead();
            long currentKey = this.keys[index];
            Object currentValue = this.values[index];
            if (!this.lock.validate(readStamp)) {
                readStamp = this.lock.readLock();
                currentKey = this.keys[index];
                currentValue = this.values[index];
            }
            if (currentKey == key && currentValue != null) {
                LockUtil.unlockIfRead(this.lock, readStamp);
                return (T)currentValue;
            }
            long writeStamp = this.lock.tryConvertToWriteLock(readStamp);
            try {
                if (writeStamp == 0L) {
                    writeStamp = LockUtil.convertToWrite(this.lock, readStamp);
                    if (this.keys[index] == key && this.values[index] != null) {
                        Object object = this.values[index];
                        return (T)object;
                    }
                }
                T newValue = function.apply(seed, key);
                this.keys[index] = key;
                this.values[index] = newValue;
                T t = newValue;
                return t;
            }
            finally {
                this.lock.unlockWrite(writeStamp);
                this.onRemove((T) currentValue);
            }
        }
    }
}
