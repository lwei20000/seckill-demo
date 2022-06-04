package com.example.seckilldemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.TOrder;
import com.example.seckilldemo.entity.TSeckillGoods;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.mapper.TOrderMapper;
import com.example.seckilldemo.mapper.TSeckillOrderMapper;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.service.ITSeckillGoodsService;
import com.example.seckilldemo.service.ITSeckillOrderService;
import com.example.seckilldemo.utils.MD5Util;
import com.example.seckilldemo.utils.UUIDUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.OrderDeatilVo;
import com.example.seckilldemo.vo.RespBeanEnum;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 服务实现类
 *
 * @author LiChao
 * @since 2022-03-03
 */
@Service
@Primary
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder> implements ITOrderService {

    @Autowired
    private ITSeckillGoodsService itSeckillGoodsService;
    @Autowired
    private TOrderMapper tOrderMapper;
    @Autowired
    private ITSeckillOrderService itSeckillOrderService;
    @Autowired
    private ITGoodsService itGoodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 秒杀
     * mac优化前QPS 1657
     *
     * @param user    用户对象
     * @param goodsVo 商品对象
     * @return
     */
    @Transactional
    @Override
    public TOrder secKill(TUser user, GoodsVo goodsVo) {

        // 取得秒杀商品
        TSeckillGoods seckillGoods = itSeckillGoodsService.getOne(new QueryWrapper<TSeckillGoods>().eq("goods_id", goodsVo.getId()));

        // 设置库存
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);

        // 未解决库存超卖
        //itSeckillGoodsService.updateById(seckillGoods);

        // 解决库存超卖(有问题)
        /*boolean seckillGoodsResult = itSeckillGoodsService.update(new UpdateWrapper<TSeckillGoods>()
                .set("stock_count", seckillGoods.getStockCount())  // 设置库存
                .eq("goods_id", seckillGoods.getId())                    // 条件：根据id更新
                .gt("stock_count", 0)                              // 确保库存大于0
        );

        if(!seckillGoodsResult) {
            return null;
        }
        */

        // 解决库存超卖(有问题)
        boolean seckillGoodsResult = itSeckillGoodsService.update(new UpdateWrapper<TSeckillGoods>()
                .setSql("stock_count = stock_count - 1")           // 设置库存
                .eq("goods_id", seckillGoods.getId())             // 条件：根据id更新
                .gt("stock_count", 0)                   // 确保库存大于0
        );
        if(!seckillGoodsResult) {
            return null;
        }

        // 解决重复秒杀：同一个用户秒杀多个商品，是通过数据库的唯一索引来解决的。

        //生成订单
        TOrder tOrder = new TOrder();
        tOrder.setUserId(user.getId());
        tOrder.setGoodsId(goodsVo.getId());
        tOrder.setDeliveryAddrId(0L);
        tOrder.setGoodsName(goodsVo.getGoodsName());
        tOrder.setGoodsCount(1);
        tOrder.setGoodsPrice(seckillGoods.getSeckillPrice());
        tOrder.setOrderChannel(1);
        tOrder.setStatus(0);
        tOrder.setCreateDate(new Date());
        tOrderMapper.insert(tOrder);

        //生成秒杀订单
        TSeckillOrder tSeckillOrder = new TSeckillOrder();
        tSeckillOrder.setUserId(user.getId());
        tSeckillOrder.setOrderId(tOrder.getId());
        tSeckillOrder.setGoodsId(goodsVo.getId());
        itSeckillOrderService.save(tSeckillOrder);

        // 秒杀订单存储到redis中
        redisTemplate.opsForValue().set("order" + user.getId()+":"+goodsVo.getId(), tSeckillOrder);

        return tOrder;
    }

    @Override
    public OrderDeatilVo detail(Long orderId) {
        if (orderId == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }
        TOrder tOrder = tOrderMapper.selectById(orderId);
        GoodsVo goodsVobyGoodsId = itGoodsService.findGoodsVobyGoodsId(tOrder.getGoodsId());
        OrderDeatilVo orderDeatilVo = new OrderDeatilVo();
        orderDeatilVo.setTOrder(tOrder);
        orderDeatilVo.setGoodsVo(goodsVobyGoodsId);
        return orderDeatilVo;
    }

    @Override
    public String createPath(TUser user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodsId, str, 1, TimeUnit.MINUTES);
        return str;
    }

    @Override
    public boolean checkPath(TUser user, Long goodsId, String path) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(path)) {
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodsId);
        return path.equals(redisPath);
    }

    @Override
    public boolean checkCaptcha(TUser user, Long goodsId, String captcha) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(captcha)) {
            return false;
        }
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
        return captcha.equals(redisCaptcha);
    }
}
