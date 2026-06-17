package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.Project;
import com.jlvtc.researchportrait.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProjectService {
    @Autowired
    private ProjectRepository projectRepository;

    public Project save(Project project) {
        return projectRepository.save(project);
    }

    public List<Project> getAll() {
        return projectRepository.findAll();
    }
}