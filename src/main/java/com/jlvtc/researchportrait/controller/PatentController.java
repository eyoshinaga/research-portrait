package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Patent;
import com.jlvtc.researchportrait.service.PatentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/patent")
public class PatentController {
    @Autowired
    private PatentService patentService;

    @PostMapping("/save")
    public Patent save(@RequestBody Patent patent) {
        return patentService.save(patent);
    }

    @GetMapping("/all")
    public List<Patent> getAll() {
        return patentService.getAll();
    }
}