package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    @Autowired
    private InstitutionService institutionService;

    /**
     * 获取机构竞争力雷达图数据
     */
    @GetMapping("/radar/{id}")
    public Map<String, Object> getRadar(@PathVariable Long id) {
        return institutionService.getCompetitivenessRadar(id);
    }
}