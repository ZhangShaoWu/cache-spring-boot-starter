package com.platform.framework.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CaffeineOpenService<T> {


    private Cache<String, T> cache = null;

    /**
     * 初始化
     * @param maxSize
     * @param duration
     */
    public void init(Integer maxSize, Duration duration) {
        cache = Caffeine.newBuilder().expireAfterWrite(duration.toMillis(), TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build();
    }

    /**
     * put
     * @param key
     * @param value
     */
    public void put(String key, T value) {
        cache.put(key, value);
    }

    /**
     * get
     * @param key
     * @param supplier
     * @return
     */
    public T get(String key, Function<String, T> supplier) {
        return cache.get(key, t -> supplier.apply(key));
    }

    /**
     * remove
     * @param key
     */
    public void remove(String key) {
        cache.invalidate(key);
    }

    /**
     * clear
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * exist
     * @param key
     * @return
     */
    public boolean exist(String key) {
        return cache.getIfPresent(key) != null;
    }

}
