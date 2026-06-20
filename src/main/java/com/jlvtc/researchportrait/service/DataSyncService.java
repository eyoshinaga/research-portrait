package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final ResearcherRepository researcherRepo;
    private final PaperRepository paperRepo;
    private final PatentRepository patentRepo;
    private final ProjectRepository projectRepo;
    private final InstitutionRepository institutionRepo;
    private final InfluenceCalculatorService calculatorService;

    /**
     * 全量同步：从 MySQL 读取数据并写入 Neo4j
     * 在实际生产中，这应该是一个增量同步或基于事件的同步
     */
    @Transactional
    public void syncAllToNeo4j() {
        // 1. 同步机构
        List<Institution> institutions = institutionRepo.findAll();
        institutionRepo.saveAll(institutions);

        // 2. 同步科研人员并计算影响力
        List<Researcher> researchers = researcherRepo.findAll();
        for (Researcher r : researchers) {
            // 处理隶属关系
            if (r.getInstitution() != null && r.getInstitution().getId() != null) {
                Institution inst = institutionRepo.findById(r.getInstitution().getId()).orElse(null);
                if (inst != null) {
                    r.setInstitution(inst);
                }
            }
            
            // 动态计算影响力指数
            double index = calculatorService.calculateInfluenceIndex(
                r, r.getPapers(), r.getPatents(), 
                r.getChargeProjects() // 仅统计负责的项目
            );
            r.setInfluenceIndex(index);
        }
        researcherRepo.saveAll(researchers);

        // 3. 同步论文及作者关系
        List<Paper> papers = paperRepo.findAll();
        paperRepo.saveAll(papers);

        // 4. 同步专利及发明人关系
        List<Patent> patents = patentRepo.findAll();
        patentRepo.saveAll(patents);

        // 5. 同步项目及成员关系
        List<Project> projects = projectRepo.findAll();
        projectRepo.saveAll(projects);
        
        System.out.println("✅ 数据同步完成：MySQL -> Neo4j");
    }
}
