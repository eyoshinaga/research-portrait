package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Institution;
import com.jlvtc.researchportrait.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    @Autowired
    private InstitutionService institutionService;

    @PostMapping("/save")
    public Institution save(@RequestBody Institution institution) {
        return institutionService.save(institution);
    }

    @GetMapping("/all")
    public List<Institution> getAll() {
        return institutionService.getAll();
    }
}