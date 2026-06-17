package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.service.DataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private DataSyncService dataSyncService;

    @GetMapping("/hello")
    public String hello() {
        return "科研人员画像系统启动成功！端口：8089";
    }

    @GetMapping("/sync")
    public String sync() {
        dataSyncService.syncAllToNeo4j();
        return "数据同步任务已触发";
    }
}