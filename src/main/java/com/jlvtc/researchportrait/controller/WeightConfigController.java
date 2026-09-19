package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.WeightConfig;
import com.jlvtc.researchportrait.service.WeightConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 动态权重配置 管理接口（管理员后台）
 * 支持查看各学科权重模板、动态调整权重系数与时间衰减系数、恢复默认模板。
 */
@RestController
@RequestMapping("/api/weight-config")
@RequiredArgsConstructor
public class WeightConfigController {

    private final WeightConfigService weightConfigService;

    /** 查询全部学科权重模板 */
    @GetMapping("/list")
    public List<WeightConfig> list() {
        return weightConfigService.listAll();
    }

    /** 更新单个学科的权重配置 */
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody WeightConfig config) {
        try {
            return ResponseEntity.ok(weightConfigService.update(config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** 恢复默认权重模板 */
    @PostMapping("/reset")
    public List<WeightConfig> reset() {
        return weightConfigService.resetToDefaults();
    }
}
