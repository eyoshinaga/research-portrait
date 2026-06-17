package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Paper;
import com.jlvtc.researchportrait.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/paper")
public class PaperController {
    @Autowired
    private PaperService paperService;

    @PostMapping("/save")
    public Paper save(@RequestBody Paper paper) {
        return paperService.save(paper);
    }

    @GetMapping("/all")
    public List<Paper> getAll() {
        return paperService.getAll();
    }
}