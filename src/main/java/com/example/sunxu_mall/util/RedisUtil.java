package com.example.sunxu_mall.util;


import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author sunxu
 */
@Component
public class RedisUtil {
    private final RedissonClient redissonClient;


    public RedisUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 设置字符串值
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 获取字符串值
     *
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 设置带过期时间的字符串值
     *
     * @param key        键
     * @param value      值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, expireTime, timeUnit);
    }

    /**
     * 递增1
     *
     * @param key 键
     * @return 递增后的值
     */
    public long incr(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }

    /**
     * 递增指定值
     *
     * @param key   键
     * @param delta 递增的值
     * @return 递增后的值
     */
    public long incrBy(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(delta);
    }

    /**
     * 递减1
     *
     * @param key 键
     * @return 递减后的值
     */
    public long decr(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.decrementAndGet();
    }

    /**
     * 递减指定值
     *
     * @param key   键
     * @param delta 递减的值
     * @return 递减后的值
     */
    public long decrBy(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(-delta);
    }

    /**
     * 仅当键不存在时设置值（原子操作，用于分布式锁/幂等）
     *
     * @param key        键
     * @param value      值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return true=设置成功（键原先不存在），false=键已存在
     */
    public boolean setIfAbsent(String key, String value, long expireTime, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        Duration duration = Duration.ofMillis(timeUnit.toMillis(expireTime));
        return bucket.setIfAbsent(value, duration);
    }

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    public void delete(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }

}
