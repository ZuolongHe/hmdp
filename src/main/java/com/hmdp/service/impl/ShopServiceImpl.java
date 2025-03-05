package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询商户信息并缓存
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 1.根据id从redis中查询商铺缓存
        String entries = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2.未命中，则从数据库中查询
        if (StrUtil.isBlank(entries)){
            Shop shop = getById(id);
            // 3.判断商铺是否存在，存在则将商铺信息写入redis，否则返回404
            if (shop == null) {
                return Result.fail("404");
            }
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop));
            stringRedisTemplate.expire(CACHE_SHOP_KEY + id, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        }else {
            // 4.命中则返回商铺信息,将Json转为Shop对象
            Shop shop = JSONUtil.toBean(entries, Shop.class);
            return Result.ok(shop);
        }

    }
}

