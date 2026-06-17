package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Researcher;
import com.jlvtc.researchportrait.service.ResearcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科研人员 接口
 */
@RestController
@RequestMapping("/api/researcher")
public class ResearcherController {

    @Autowired
    private ResearcherService researcherService;

    /**
     * 添加/更新科研人员
     */
    @PostMapping("/save")
    public Researcher save(@RequestBody Researcher researcher) {
        return researcherService.saveResearcher(researcher);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/get/{id}")
    public Researcher getById(@PathVariable Long id) {
        return researcherService.findById(id);
    }

    /**
     * 查询所有
     */
    @GetMapping("/all")
    public List<Researcher> getAll() {
        return researcherService.findAll();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        researcherService.deleteById(id);
        return "删除成功";
    }
}