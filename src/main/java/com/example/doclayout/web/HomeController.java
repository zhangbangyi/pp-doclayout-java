package com.example.doclayout.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web 首页控制器，将根路径转发到静态上传页面。
 */
@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        // 使用 forward 保留当前请求上下文，同时交给 Spring 静态资源处理器返回 index.html。
        return "forward:/index.html";
    }
}
