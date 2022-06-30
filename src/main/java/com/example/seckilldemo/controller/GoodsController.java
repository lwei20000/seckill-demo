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

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

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
     * QPS：885
     *
     * @param model
     * @param user
     * @return
     */
    @ApiOperation("商品列表")
    @RequestMapping(value = "/toList1.3", method = RequestMethod.GET)
    public String toList(Model model, TUser user) {

        List<GoodsVo> godsVoList = itGoodsService.findGoodsVo();
        model.addAttribute("user", user);
        model.addAttribute("goodsList", godsVoList);

        System.out.println("===================toList1。3>" + godsVoList.size());
        return "goodsList";
    }

    /**
     * 商品列表页1。4（页面缓存）
     * QPS：2426
     *
     * @param model
     * @param user
     * @return
     */
    @ApiOperation("商品列表")
    @RequestMapping(value = "/toList1.4", produces = "text/html;charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public String toList(Model model, TUser user, HttpServletRequest request, HttpServletResponse response) {

        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if(!StringUtils.isEmpty(html)) {
            return html;
        }

        List<GoodsVo> godsVoList = itGoodsService.findGoodsVo();
        model.addAttribute("user", user);
        model.addAttribute("goodsList", godsVoList);

        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        if(!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html,60, TimeUnit.SECONDS);
        }

        System.out.println("===================toList1。4>" + godsVoList.size());
        return html;
    }


    /**
     * 商品详情页1.1
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @ApiOperation("商品详情")
    @RequestMapping(value = "/toDetail1.1/{goodsId}", method = RequestMethod.GET)
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

    /**
     * 商品详情页1.2（页面缓存）
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @ApiOperation("商品详情")
    @RequestMapping(value = "/toDetail1.2/{goodsId}", produces = "text/html;charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public String toDetail(Model model,TUser user, @PathVariable Long goodsId, HttpServletRequest request, HttpServletResponse response) {

        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail" + goodsId);
        if(!StringUtils.isEmpty(html)) {
            return html;
        }

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

        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", context);
        if(!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsDetail" + goodsId, html,60, TimeUnit.SECONDS);
        }

        return html;
    }

    /**
     * 商品详情页1.3 （页面静态化（一共三个页面：商品详情、秒杀页面、订单详情））
     *
     * @param user
     * @param goodsId
     * @return
     */
    @ApiOperation("商品详情")
    @RequestMapping(value = "/toDetail2/{goodsId}")
    @ResponseBody
    public RespBean toDetail2(TUser user, @PathVariable Long goodsId) {

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

        DetailVo detailVo = new DetailVo();
        detailVo.setTUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSecKillStatus(remainSeconds);
        detailVo.setRemainSeconds(secKillStatus);
        return RespBean.success(detailVo);
    }






















}
