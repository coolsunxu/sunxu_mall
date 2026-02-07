package com.example.sunxu_mall.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;

/**
 * Snowflake ID Worker
 * <p>
 * 分布式唯一ID生成器，基于Twitter Snowflake算法实现
 * <p>
 * 结构：
 * - 1位符号位（始终为0）
 * - 41位时间戳（毫秒级，约69年）
 * - 10位工作机器ID（5位datacenterId + 5位workerId）
 * - 12位序列号（每毫秒4096个ID）
 *
 * @author sunxu
 * @version 1.0
 * @date 2026/02/06
 */
@Slf4j
@Component
public class SnowflakeIdWorker {

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * workerId占用的位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * datacenterId占用的位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号占用的位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 支持的最大workerId
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 支持的最大datacenterId
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * workerId左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * datacenterId左移位数
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 序列号掩码
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @PostConstruct
    public void init() {
        this.workerId = getMachineId();
        this.datacenterId = getDatacenterId();
        log.info("Snowflake initialized, workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * 获取下一个ID
     *
     * @return 分布式唯一ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards, refuse to generate id");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 获取下一个ID（字符串）
     *
     * @return 分布式唯一ID字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private long getMachineId() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] ipAddressByteArray = address.getAddress();
            long ip = 0;
            for (byte b : ipAddressByteArray) {
                ip = (ip << 8) | (b & 0xFF);
            }
            return ip % (MAX_WORKER_ID + 1);
        } catch (UnknownHostException e) {
            return new SecureRandom().nextInt((int) (MAX_WORKER_ID + 1));
        }
    }

    private long getDatacenterId() {
        int random = new SecureRandom().nextInt((int) (MAX_DATACENTER_ID + 1));
        return Math.abs(random);
    }
}