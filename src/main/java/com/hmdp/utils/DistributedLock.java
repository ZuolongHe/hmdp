package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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

    public DistributedLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    // 获取分布式锁
    @Override
    public boolean tryLock(long timeoutSec) {
        String threadName = Thread.currentThread().getName();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(preName + this.name + ":", threadName, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lock);
    }

    // 释放分布式锁
    @Override
    public void unlock() {
        stringRedisTemplate.delete(preName + this.name + ":");
    }
}
