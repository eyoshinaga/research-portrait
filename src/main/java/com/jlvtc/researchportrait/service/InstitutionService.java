package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepo;
    private final ResearcherRepository researcherRepo;
    private final PaperRepository paperRepo;
    private final PatentRepository patentRepo;
    private final ProjectRepository projectRepo;

    /**
     * 获取指定机构的竞争力雷达图数据 (6维度)
     */
    public Map<String, Object> getCompetitivenessRadar(Long instId) {
        Institution inst = institutionRepo.findById(instId).orElse(null);
        if (inst == null) return Collections.emptyMap();

        // 获取该机构下所有科研人员
        List<Researcher> researchers = researcherRepo.findAll().stream()
            .filter(r -> r.getInstitution() != null && r.getInstitution().getId().equals(instId))
            .collect(Collectors.toList());

        Set<Long> researcherIds = researchers.stream().map(Researcher::getId).collect(Collectors.toSet());

        // 1. 科研规模 (人员数 + 成果数)
        long paperCount = paperRepo.findAll().stream().filter(p -> hasAuthor(p, researcherIds)).count();
        long patentCount = patentRepo.findAll().stream().filter(p -> hasInventor(p, researcherIds)).count();
        long projectCount = projectRepo.findAll().stream().filter(p -> hasMember(p, researcherIds)).count();
        double scale = researchers.size() + paperCount + patentCount + projectCount;

        // 2. 科研质量 (平均IF + 项目级别加分)
        double avgIf = paperRepo.findAll().stream()
            .filter(p -> hasAuthor(p, researcherIds) && p.getImpactFactor() != null)
            .mapToDouble(Paper::getImpactFactor).average().orElse(0);
        
        long nationalProjects = projectRepo.findAll().stream()
            .filter(p -> hasMember(p, researcherIds) && "国家级".equals(p.getProjLevel())).count();
        double quality = (avgIf * 10) + (nationalProjects * 5); // 加权打分

        // 3. 学术影响力 (平均影响力指数)
        double influence = researchers.stream()
            .mapToDouble(r -> r.getInfluenceIndex() != null ? r.getInfluenceIndex() : 0)
            .average().orElse(0);

        // 4. 科研效率 (人均成果)
        double efficiency = researchers.isEmpty() ? 0 : (paperCount + patentCount) / (double) researchers.size();

        // 5. 国际合作 (模拟：根据机构类型或预设值)
        double cooperation = "大学".equals(inst.getType()) ? 75.0 : 40.0; 

        // 6. 成果转化 (专利数 * 权重)
        double conversion = patentCount * 2.5;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("institutionName", inst.getName());
        result.put("dimensions", Map.of(
            "科研规模", Math.min(scale / 10, 100), // 归一化处理，满分100
            "科研质量", Math.min(quality, 100),
            "学术影响力", Math.min(influence * 10, 100),
            "科研效率", Math.min(efficiency * 5, 100),
            "国际合作", cooperation,
            "成果转化", Math.min(conversion, 100)
        ));

        return result;
    }

    // 辅助方法：判断论文是否包含该机构的研究人员
    private boolean hasAuthor(Paper paper, Set<Long> ids) {
        // 实际生产中应通过关联表查询，这里简化处理
        return true; 
    }
    private boolean hasInventor(Patent patent, Set<Long> ids) { return true; }
    private boolean hasMember(Project project, Set<Long> ids) { return true; }
}
