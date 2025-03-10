package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商户信息并缓存到redis中
     * @param id
     * @return
     */
    Result queryById(Long id);

    Result updateByIdCache(Shop shop);

    Result queryByIdThrowTop(Long id);

    Result logicExpire(Long id);
}
