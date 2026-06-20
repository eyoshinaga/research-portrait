package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    @Autowired
    private InstitutionService institutionService;

    /**
     * 查询所有机构列表（简化，用于前端复选框）
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return institutionService.findAllSimple();
    }

    /**
     * 获取机构竞争力雷达图数据
     */
    @GetMapping("/radar/{id}")
    public Map<String, Object> getRadar(@PathVariable Long id) {
        return institutionService.getCompetitivenessRadar(id);
    }

    /**
     * 多机构竞争力对比
     */
    @PostMapping("/compare")
    public List<Map<String, Object>> compare(@RequestBody List<Long> ids) {
        return institutionService.compareInstitutions(ids);
    }
}