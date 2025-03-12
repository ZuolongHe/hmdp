package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2025/3/6 22:04
 * @Description 生成全局唯一id
 *
 */
@Component
public class RedisIdWorker {

    // 起始时间戳
    private static final long START_TIME = 1577836800L;

    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 生成全局唯一id
    public long onlyId(String keyValue){
        // 1.生成时间戳
        long distanceTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - START_TIME;
        // 2.生成序列号
        String yyyyMMdd = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long increment = stringRedisTemplate
                .opsForValue().increment("inc:" + keyValue + ":" + yyyyMMdd);
        // 3.拼接时间戳+序列号
        return distanceTime << COUNT_BITS | increment;
    }

    public static void main(String[] args) {
        LocalDateTime of = LocalDateTime
                .of(2020, 1, 1, 0, 0, 0);
        long second = of.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);
    }






}
