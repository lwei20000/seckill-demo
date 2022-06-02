package com.example.seckilldemo.controller;

import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITUserService;
import com.example.seckilldemo.vo.DetailVo;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.catalina.User;
import org.omg.CORBA.TIMEOUT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品
 *
 * @author: LC
 * @date 2022/3/2 5:56 下午
 * @ClassName: GoodsController
 */
@Controller
@RequestMapping("goods")
@Api(value = "商品", tags = "商品")
public class GoodsController {

    @Autowired
    private ITUserService itUserService;

    @Autowired
    private ITGoodsService itGoodsService;

    /**
     * 商品列表页1。1
     * @param session
     * @param model
     * @param ticket
     * @return
     */
    @RequestMapping(value = "/toList1.1")
    public String toList(HttpSession session, Model model, @CookieValue("userTicket") String ticket) {

        if(StringUtils.isEmpty(ticket)) {
            return "login";
        }

        // 通过session获取用户信息
         User user = (User)session.getAttribute(ticket);
        if(null == user) {
            return "login";
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", itGoodsService.findGoodsVo());
        return "goodsList";
    }

    /**
     * 商品列表页1。2（使用redis获取用户信息）
     * @param model
     * @param ticket
     * @return
     */
    @RequestMapping(value = "/toList1.2")
    public String toList(HttpServletRequest request, HttpServletResponse response,Model model, @CookieValue("userTicket") String ticket) {

        if(StringUtils.isEmpty(ticket)) {
            return "login";
        }

        // 通过redis获取用户信息
        TUser user = itUserService.getUserByCookie(ticket, request, response);

        if(null == user) {
            return "login";
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", itGoodsService.findGoodsVo());
        return "goodsList";
    }

    /**
     * 商品列表页1。3
     * @param model
     * @param user
     * @return
     */
    @ApiOperation("商品列表")
    @RequestMapping(value = "/toList1.3", produces = "text/html;charset=utf-8", method = RequestMethod.GET)
    public String toList(Model model, TUser user) {

        List<GoodsVo> godsVoList = itGoodsService.findGoodsVo();
        model.addAttribute("user", user);
        model.addAttribute("goodsList", godsVoList);

        System.out.println("===================>" + godsVoList.size());
        return "goodsList";
    }


    /**
     * 商品详情页
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @ApiOperation("商品详情")
    @RequestMapping(value = "/toDetail/{goodsId}", method = RequestMethod.GET)
    public String toDetail(Model model,TUser user, @PathVariable Long goodsId) {

        model.addAttribute("user", user);
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();

        // 秒杀状态
        int secKillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;

        if(nowDate.before(startDate)) {
            secKillStatus = 0; // 0秒杀未开始
            remainSeconds = (int)(startDate.getTime() - nowDate.getTime()) / 1000;
        } else if(nowDate.after(endDate)) {
            secKillStatus = 2; // 2秒杀已结束
            remainSeconds = -1;
        } else {
            secKillStatus = 1; // 秒杀进行中
            remainSeconds = 0;
        }
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsVo);
        return "goodsDetail";
    }
























}
