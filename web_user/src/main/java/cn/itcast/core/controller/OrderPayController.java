package cn.itcast.core.controller;


import cn.itcast.core.pojo.entity.BuyerCart;

import cn.itcast.core.service.CartService;

import com.alibaba.dubbo.config.annotation.Reference;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orderPay")
public class OrderPayController {

    @Reference
    private CartService cartService;


    @RequestMapping("/findCartList")
    public List<BuyerCart> findCartList() {
        //1. 获取当前登录用户名称
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        List<BuyerCart> cartList = cartService.getCartListFromRedis(userName);

        return cartList;

    }
}
