package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2025/3/29 20:31
 * @Description
 */
public class DistributedLock implements ILock{

    // 业务名
    private String name;

    private StringRedisTemplate stringRedisTemplate;

    private static final String preName = "lock:";

    private static final String index_name = UUID.randomUUID().toString() + "-";

    public DistributedLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    // 获取分布式锁
    @Override
    public boolean tryLock(long timeoutSec) {
        long threadName = Thread.currentThread().getId();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(preName + name + ":", index_name + threadName, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lock);
    }

    // 释放分布式锁
    @Override
    public void unlock() {
        // 获取此时分布式锁
        String s = stringRedisTemplate.opsForValue().get(preName + name + ":");
        // 判断 是则释放锁
        if (s.equals(index_name + Thread.currentThread().getId())){
            stringRedisTemplate.delete(preName + name + ":");
        }

    }
}
