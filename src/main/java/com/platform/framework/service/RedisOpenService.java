package com.platform.framework.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RedisOpenService<T> {


    @Autowired
    private RedisTemplate<String, T> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 设置缓存
     *
     * @param key
     * @param value
     * @param duration
     */
    public void set(String key, T value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 根据给定的键从Redis中获取值。
     *
     * @param key 键的名称，用于在Redis中唯一标识一个值。
     * @return 返回一个Optional对象，其中包含从Redis中获取的值。如果键不存在，则Optional对象为空。
     * 使用Optional是为了避免直接返回null，从而提高代码的可读性和安全性。
     */
    public Optional<T> get(String key) {
        T t = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(t);
    }

    /**
     * hset
     *
     * @param key
     * @param value
     */
    public void hset(String key, T value, Duration duration) {
        Optional.ofNullable(value)
                .map(e -> {
                    Map<String, Object> map = BeanUtil.beanToMap(e);
                    Map<String, String> result = new HashMap<>();
                    map.forEach((k, v) -> {
                        Optional.ofNullable(v).ifPresent(val -> result.put(k, v.toString()));
                    });
                    return result;
                })
                .ifPresent(map -> {
                    redisTemplate.opsForHash().putAll(key, map);
                    expire(key, duration);
                });
    }

    /**
     * hset
     *
     * @param key
     * @param hashKey
     * @param value
     */
    public void hset(String key, String hashKey, T value, Duration duration) {
        redisTemplate.opsForHash().put(key, hashKey, value);
        expire(key, duration);
    }

    /**
     * hget
     *
     * @param key
     * @param hashKey
     * @return
     */
    public Optional<T> hget(String key, String hashKey) {
        T t = (T) redisTemplate.opsForHash().get(key, hashKey);
        return Optional.ofNullable(t);
    }

    /**
     * hget
     *
     * @param key
     * @param clazz
     * @return
     */
    public Optional<T> hget(String key, Class<T> clazz) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return Optional.of(BeanUtil.mapToBean(entries, clazz, true));
    }

    /**
     * incr
     *
     * @param key
     * @param duration
     * @return
     */
    public Long incr(String key, Duration duration) {
        Long result = redisTemplate.opsForValue().increment(key);
        expire(key, duration);
        return result;
    }


    /**
     * incrby
     *
     * @param key
     * @param delta
     * @param duration
     * @return
     */
    public Long incrby(String key, int delta, Duration duration) {
        Long increment = redisTemplate.opsForValue().increment(key, delta);
        expire(key, duration);
        return increment;
    }

    /**
     * exist
     *
     * @param key
     * @return
     */
    public boolean exist(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * not exist
     *
     * @param key
     * @return
     */
    public boolean notExist(String key) {
        return !exist(key);
    }

    /**
     * expire
     *
     * @param key
     * @param duration
     */
    public void expire(String key, Duration duration) {
        redisTemplate.expire(key, duration);
    }

    /**
     * del
     *
     * @param key
     */
    public void del(String key) {
        redisTemplate.delete(key);
    }

    /**
     * setNx
     *
     * @param key
     * @param val
     * @param duration
     * @return
     */
    public boolean setNx(String key, T val, Duration duration) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, val);
        redisTemplate.expire(key, duration.toMillis(), TimeUnit.MILLISECONDS);
        return BooleanUtil.isTrue(result);
    }

    /**
     * 幂等性处理
     * @param key
     * @param val
     * @param duration
     * @return
     */
    public boolean idempotent(String key, T val,Duration duration) {
        boolean b = setNx(key, val, duration);
        return BooleanUtil.isFalse(b);
    }

    /**
     * lock
     *
     * @param key
     * @param function
     */
    public T lock(String key, Supplier<T> function) {
        RLock lock = redissonClient.getLock(key);
        try {
            lock.lock();
            return function.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * tryLock
     *
     * @param key
     * @param duration
     * @param function
     * @return
     */
    public T tryLock(String key, Duration duration, Supplier<T> function) {
        RLock lock = redissonClient.getLock(key);
        try {
            boolean b = lock.tryLock(duration.toMillis(), TimeUnit.MILLISECONDS);
            if (b) {
                return function.get();
            }
            return function.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
