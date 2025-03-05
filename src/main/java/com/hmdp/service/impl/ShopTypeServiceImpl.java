package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Dragon
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 查询商铺所有类型
     * @return
     */
    @Override
    public List<ShopType> getAllType() {
        // 1.redis数据库查询分类信息
        Set<String> keys = stringRedisTemplate.keys(LOCK_SHOP_KEY + "*");
        List<String> list = stringRedisTemplate.opsForValue().multiGet(keys);
        // 2.若redis数据库没有数据，则在数据库查询
        if (list.isEmpty()){
            List<ShopType> allShopType = query().orderByAsc("sort").list();
            // 3.将allShopType依此存入redis数据库
            allShopType.forEach(shopType -> {
                stringRedisTemplate.opsForValue().
                        set(LOCK_SHOP_KEY + shopType.getId(),JSONUtil.toJsonStr(shopType));
            });
            return allShopType;
        }
        List<ShopType> allShopType = new ArrayList<>();
        list.forEach(s -> {
            ShopType shopType = JSONUtil.toBean(s, ShopType.class);
            allShopType.add(shopType);
        });
        return allShopType;
    }
}
