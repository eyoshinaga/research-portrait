package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Project;
import com.jlvtc.researchportrait.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/project")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @PostMapping("/save")
    public Project save(@RequestBody Project project) {
        return projectService.save(project);
    }

    @GetMapping("/all")
    public List<Project> getAll() {
        return projectService.getAll();
    }
}