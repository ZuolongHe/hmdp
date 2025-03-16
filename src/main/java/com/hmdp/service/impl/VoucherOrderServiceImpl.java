package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        // 5.库存充足，扣减库存
        boolean bo = iSeckillVoucherService.update()
                .set("stock", seckillVoucher.setStock(seckillVoucher.getStock() - 1))
                .eq("voucher_id", seckillVoucher.getVoucherId()).update();
        if (!bo){
            return Result.fail("库存不足");
        }
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        Long id = UserHolder.getUser().getId();
        long orderid = redisIdWorker.onlyId("Order");
        voucherOrder.setId(orderid)
                .setUserId(id)
                .setVoucherId(seckillVoucher.getVoucherId())
                .setPayType(1)
                .setStatus(1);
        // 保存订单id
        save(voucherOrder);
        return Result.ok(orderid);
    }
}
