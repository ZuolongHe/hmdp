package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.DistributedLock;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.sun.xml.internal.bind.v2.TODO;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.security.sasl.RealmCallback;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> voucher_Script;

    static {
        voucher_Script = new DefaultRedisScript<>();
        voucher_Script.setLocation(new ClassPathResource("seckill.lua"));
        voucher_Script.setResultType(Long.class);
    }

    /**
     * 抢购优惠券
     * @param voucherId
     * @return
     */
    @Transactional
    @Override
    public Result getVoucher(Long voucherId) {
        // 1.根据id查询优惠券信息
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        LocalDateTime now = LocalDateTime.now();
        // 3.秒杀未开始
        if (now.isBefore(seckillVoucher.getBeginTime()) || now.isAfter(seckillVoucher.getEndTime())){
            return Result.fail("秒杀未开始");
        }
        // 4.秒杀已开始,判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        // 用户获取分布式锁
        DistributedLock distributedLock = new DistributedLock("VoucherOrder:" + userId, stringRedisTemplate);
        boolean bool = distributedLock.tryLock(200);
        if (bool == false){
            return Result.fail("获取分布式锁失败");
        }
        try {
            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return iVoucherOrderService.createVoucher(voucherId);
        } finally {
            // 释放锁
            distributedLock.unlock();
        }

//        synchronized (userId.toString().intern()) {
//            // 获取代理对象(事务)
//            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
//            return iVoucherOrderService.createVoucher(voucherId);
//        }

    }

    /**
     * 基于Redis实现异步秒杀
     * author: @hezuolong
     */
    public Result getVoucherByRedis(Long voucherId) {
        // 执行lua脚本
        List<String> keys = Arrays.asList(
                RedisConstants.SECKILL_STOCK_KEY + voucherId,
                RedisConstants.SECKILL_ORDER     + voucherId
        );
        Long execute = stringRedisTemplate.execute(
                voucher_Script,
                keys,
                voucherId, UserHolder.getUser().getId()
        );
        // 判断结果是否为0
        int lua = execute.intValue();
        if (lua != 0){
            if (lua == 1) {
                return Result.fail("库存不足！");
            }else if (lua == 2){
                return Result.fail("您已下单！");
            }
        }
        // TODO 将优惠券id、用户id、订单id存入阻塞队列
        return Result.ok("成功下单！");
    }


    /**
     * 创建购买优惠券的订单
     * @param voucherId
     * @return
     */
    @Transactional
    public Result createVoucher(Long voucherId){
        // 根据优惠券id和用户id查询订单是否存在
        Long count = query().eq("user_id", UserHolder.getUser().getId()).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("已存在！");
        }

        // 5.乐观锁 解决库存超卖问题 CAS时间戳机制
        boolean bo = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!bo) {
            return Result.fail("库存不足");
        }
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        Long id = UserHolder.getUser().getId();
        long orderid = redisIdWorker.onlyId("Order");
        voucherOrder.setId(orderid)
                .setUserId(id)
                .setVoucherId(voucherId)
                .setPayType(1)
                .setStatus(1);
        // 保存订单id
        save(voucherOrder);
        // 保存用户id到redis数据库中 key: seckill:order: + vocuherId
        stringRedisTemplate.opsForSet().
                add(RedisConstants.SECKILL_ORDER + voucherOrder.getVoucherId(), StrUtil.toString(orderid));
        return Result.ok(orderid);
    }
}
