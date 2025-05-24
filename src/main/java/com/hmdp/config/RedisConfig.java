package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2025/4/17 20:53
 * @Description Redssion锁工具
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis地址").setPassword("");
        return Redisson.create(config);
    }
}
