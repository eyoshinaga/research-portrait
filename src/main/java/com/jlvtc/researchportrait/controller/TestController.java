package com.jlvtc.researchportrait.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping("/hello")
    public String hello() {
        return "科研人员画像系统启动成功！端口：8089";
    }
}