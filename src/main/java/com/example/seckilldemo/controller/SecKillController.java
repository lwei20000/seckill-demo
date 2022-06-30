package com.example.seckilldemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.seckilldemo.config.AccessLimit;
import com.example.seckilldemo.entity.TOrder;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.rabbitmq.MQSender;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.service.ITSeckillOrderService;
import com.example.seckilldemo.utils.JsonUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import com.example.seckilldemo.vo.SeckillMessage;
import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀
 *
 * @author: LC
 * @date 2022/3/4 11:34 上午
 * @ClassName: SeKillController
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
@Api(value = "秒杀", tags = "秒杀")
public class SecKillController implements InitializingBean {

    @Autowired
    private ITGoodsService itGoodsService;
    @Autowired
    private ITSeckillOrderService itSeckillOrderService;
    @Autowired
    private ITOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> stockScript;

    // 内存标记
    private Map<Long, Boolean> emptystockMap = new HashMap<Long, Boolean>();


    /**
     * 获取秒杀结果
     *
     * @param tUser
     * @param goodsId
     * @return orderId 成功 ；-1 秒杀失败 ；0 排队中
     * @author LiChao
     * @operation add
     * @date 7:04 下午 2022/3/8
     **/
    @ApiOperation("获取秒杀结果")
    @GetMapping("getResult")
    @ResponseBody
    public RespBean getResult(TUser tUser, Long goodsId) {
        if (tUser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = itSeckillOrderService.getResult(tUser, goodsId);
        return RespBean.success(orderId);
    }

    /**
     * 秒杀功能
     *
     * 优化以前QPS：1783
     *
     *
     * @param model
     * @param user
     * @param goodsId
     * @return java.lang.String
     * @author LC
     * @operation add
     * @date 11:36 上午 2022/3/4
     **/
    @ApiOperation("秒杀功能")
    @RequestMapping(value = "/doSeckill1.0", method = RequestMethod.POST)
    public String doSecKill(Model model, TUser user, Long goodsId) {

        if(user == null) {
            return "login";
        }

        model.addAttribute("user", user);

        // 校验库存
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }

        // 判断是否重复抢购
        TSeckillOrder seckillOrder = itSeckillOrderService.getOne(new QueryWrapper<TSeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));

        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }

        // 进行秒杀（扣减库存、生成订单、生成秒杀订单）
        TOrder tOrder = orderService.secKill(user, goodsVo);
        model.addAttribute("order", tOrder);
        model.addAttribute("goods", goodsVo);

        System.out.println("》》》》》》》》》》》》》》》》》 秒杀成功");

        // 订单详情页
        return "orderDetail";
    }

    /**
     * 秒杀功能(预减库存)
     *
     * 优化以前QPS：1783
     *
     *
     * @param model
     * @param user
     * @param goodsId
     * @return java.lang.String
     * @author LC
     * @operation add
     * @date 11:36 上午 2022/3/4
     **/
    @ApiOperation("秒杀功能")
    @RequestMapping(value = "/doSeckill2.0", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill2(Model model, TUser user, Long goodsId) {

        // 校验登陆用户
        if(user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        // 校验库存
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 判断是否重复抢购

        // 从数据库获取
        // TSeckillOrder seckillOrder = itSeckillOrderService.getOne(new QueryWrapper<TSeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));

        // 从redis获取
        TSeckillOrder seckillOrder = (TSeckillOrder)redisTemplate.opsForValue().get("order" + user.getId()+":"+goodsVo.getId());

        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }

        // 进行秒杀（扣减库存、生成订单、生成秒杀订单）
        TOrder tOrder = orderService.secKill(user, goodsVo);

        System.out.println("》》》》》》》》》》》》》》》》》 秒杀成功2.0");

        // 订单详情页
        return RespBean.success(tOrder);
    }

    /**
     * 秒杀功能3。0（服务接口优化：预减库存，异步下单，内存标记）
     *
     * 优化以前QPS：1783
     * 优化以后QPS：2666
     * 消息队列QPS：3337
     *
     * @param user
     * @param goodsId
     * @return java.lang.String
     * @author LC
     * @operation add
     * @date 11:36 上午 2022/3/4
     **/
    @ApiOperation("秒杀功能")
    @RequestMapping(value = "/doSeckill3.0", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill3(TUser user, Long goodsId) {

        // 校验登陆用户
        if(user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 判断是否重复抢购
        TSeckillOrder seckillOrder = (TSeckillOrder)redisTemplate.opsForValue().get("order" + user.getId()+":"+goodsId);
        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }

        // 通过内存标记减少redis访问
        if(emptystockMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 预减库存
        //long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        long stock = (Long)redisTemplate.execute(stockScript, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock < 0) {
            emptystockMap.put(goodsId, true); // true代表没有库存
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 下单（rabbitMQ）
        SeckillMessage seckillMessage = new SeckillMessage(user,goodsId);
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));

        System.out.println("》》》》》》》》》》》》》》》》》 秒杀成功3.0");

        // 订单详情页
        return RespBean.success(0);
    }

    /**
     * 系统初始化，把商品库存数量家在到redis中
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVoList = itGoodsService.findGoodsVo();
        if(CollectionUtils.isEmpty(goodsVoList)) {
            return;
        }
        goodsVoList.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            emptystockMap.put(goodsVo.getId(), false); // false代表有库存（预减库存的时候设置为true）
        });
    }
}
