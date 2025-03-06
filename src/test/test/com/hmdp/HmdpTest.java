package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Test
    void testRedisData(){
        shopService.shopToRedis(1L, 10L);
    }

}
