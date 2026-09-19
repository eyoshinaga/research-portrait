package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.service.GroupAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 群体对比分析 接口
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private GroupAnalysisService groupAnalysisService;

    /**
     * 多维度群体对比：按学科领域、职称级别、所属院系、人才类别聚合科研人员群体，
     * 在科研产出、影响力、合作广度等指标上进行横向对比与年度趋势分析
     */
    @GetMapping("/group-compare")
    public Map<String, Object> groupCompare(@RequestParam(defaultValue = "discipline") String dimension) {
        return groupAnalysisService.groupCompare(dimension);
    }
}
