package com.example.seckilldemo.controller;

import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.OrderDeatilVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端控制器
 *
 * @author LiChao
 * @since 2022-03-03
 */
@RestController
@RequestMapping("/order")
@Api(value = "订单", tags = "订单")
public class TOrderController {

    @Autowired
    private ITOrderService itOrderService;

    /**
     * 订单详情
     * @param tUser
     * @param orderId
     * @return
     */
    @ApiOperation("订单")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    @ResponseBody
    public RespBean detail(TUser tUser, Long orderId) {
        if(orderId == null) {
            // 订单信息不存在
            return RespBean.error(RespBeanEnum.ORDER_NOT_EXIST);
        }
        if (tUser == null) {
            // 用户信息不存在
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        // 获取订单详情
        OrderDeatilVo orderDeatilVo = itOrderService.detail(orderId);

        // 返回
        return RespBean.success(orderDeatilVo);
    }
}
