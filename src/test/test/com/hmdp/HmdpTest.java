package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Title hmdp
 * @Author hzl
 * @Date 2025/3/6 16:35
 * @Description
 */
@SpringBootTest
public class HmdpTest {

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private IShopService iShopService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Test
    void testRedisData(){
        shopService.shopToRedis(1L, 10L);
    }

    @Test
    void testOnlyId(){
        Runnable task = () -> {
            for (int i = 1; i <= 100 ; i++) {
                long order = redisIdWorker.onlyId("order");
                System.out.println("id:" + i + ":" + order);
            }
        };
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }

        executorService.shutdown();

    }

}
