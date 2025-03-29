package com.hmdp.utils;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2025/3/29 20:27
 * @Description 分布式锁接口
 */
public interface ILock {

    /**
     * 获取分布式锁
     * @param timeoutSec
     * @return 成功：true 失败： false
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
