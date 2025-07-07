package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisData;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
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
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 1.根据id从redis中查询商铺缓存
        String entries = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2.未命中，则从数据库中查询
        if (StrUtil.isBlank(entries)) {
            Shop shop = getById(id);
            // 3.判断商铺是否存在，存在则将商铺信息写入redis，否则返回404
            if (shop == null) {
                return Result.fail("404");
            }
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop));
            stringRedisTemplate.expire(CACHE_SHOP_KEY + id, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        } else {
            // 4.命中则返回商铺信息,将Json转为Shop对象
            Shop shop = JSONUtil.toBean(entries, Shop.class);
            return Result.ok(shop);
        }

    }


    /**
     * 缓存穿透解决方案，商品信息查询，缓存空值
     *
     * @param id
     * @return
     */
    public Result queryByIdCacheThrow(Long id) {
        // 1.根据id从redis中查询商铺缓存
        String entries = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2.缓存有数据，则从数据库中查询
        if (StrUtil.isNotBlank(entries)) {
            return Result.ok(JSONUtil.toBean(entries, Shop.class));
        }
        // 3.缓存没有数据
        if (entries != null) {
            return Result.ok(null);
        }
        // 4.未命中，则查询数据库
        Shop shop = getById(id);
        if (shop != null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
        return Result.ok(null);
    }


    /**
     * 缓存击穿解决方案，基于互斥锁
     */
    @Override
    public Result queryByIdThrowTop(Long id) {
        // 1.从缓存取出商铺数据
        String lockKey = "key:lock:shop:" + id;
        String shop = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2.判断缓存是否命中
        if (shop != null) {
            // 缓存命中,返回数据
            return Result.ok(JSONUtil.toBean(shop, Shop.class));
        }
        try {
            // 3.缓存未命中，获取锁
            boolean b = tryLock(lockKey);
            if (b == false) {
                // 未获取锁，休眠, 递归
                Thread.sleep(200);
                return queryByIdThrowTop(id);
            }
            // 获取锁，根据id查询数据，写入redis
            Shop idShop = getById(id);
            if (idShop == null) {
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_SHOP_TTL, TimeUnit.MINUTES);
                return Result.ok(null);
            }
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(idShop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(idShop);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unLock(lockKey);
        }
    }


    /**
     * 根据id查询商铺信息，逻辑过期解决缓存穿透
     */
    @Override
    public Result logicExpire(Long id) {
        // 1、从Redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2、缓存命中
        if (shopJson != null) {
            // 将json数据实例化为对象
            RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
            // 2.1、判断缓存是否过期，逻辑过期时间是否在当前时间后面
            if (redisData.getExpireTime().isAfter(LocalDateTime.now())){
                // 没过期，返回商铺信息
                return Result.ok(redisData.getData());
            }
            // 过期，获取互斥锁
            if (tryLock(LOCK_KEY)) {
                // 获取互斥锁成功，开启独立线程，根据id查询数据库
                new Thread(() -> {
                    try {
                        shopToRedis(id, 2L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        unLock(LOCK_KEY);
                    }
                }).start();
            }
            // 获取互斥锁失败，返回旧的店铺信息
            return Result.ok(redisData.getData());
        }
        // 3、缓存未命中，返回空
        return Result.ok(null);
    }


    /**
     * 封装含有过期时间的商铺信息Shop2Redis
     */
    public void shopToRedis(Long id, Long expireTime) {
        Shop byId = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(byId);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        stringRedisTemplate.opsForValue()
                .set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 获取互斥锁
     */
    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }


    /**
     * 释放互斥锁
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 更新商户信息
     *
     * @return
     */
    @Transactional
    @Override
    public Result updateByIdCache(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id为空！");
        }
        // 1.先更新数据库
        updateById(shop);

        // 2.再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

        return Result.ok();
    }
}

