package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.WeightConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluenceCalculatorService {

    private final WeightConfigRepository weightConfigRepo;

    /**
     * 计算单个科研人员的学术影响力指数
     */
    public double calculateInfluenceIndex(Researcher researcher, List<Paper> papers, 
                                          List<Patent> patents, List<Project> projects) {
        // 1. 获取学科权重配置（默认为通用配置）
        WeightConfig config = getWeightConfig(researcher.getDisciplineCategory());
        
        double totalScore = 0.0;

        // 2. 计算论文得分
        for (Paper p : papers) {
            double baseScore = (p.getCitedNum() != null ? p.getCitedNum() : 0) * 0.5 + 
                               (p.getImpactFactor() != null ? p.getImpactFactor() : 0) * 2.0;
            totalScore += applyDecay(baseScore, p.getPubDate(), config.getDecayRate()) * config.getPaperWeight();
        }

        // 3. 计算专利得分
        for (Patent pt : patents) {
            double baseScore = 5.0; // 专利基础分
            if ("发明专利".equals(pt.getPatentType())) baseScore = 10.0;
            totalScore += applyDecay(baseScore, pt.getGrantDate(), config.getDecayRate()) * config.getPatentWeight();
        }

        // 4. 计算项目得分
        for (Project pr : projects) {
            double baseScore = (pr.getFund() != null ? pr.getFund() : 0) * 0.1; // 经费贡献
            if ("国家级".equals(pr.getProjLevel())) baseScore *= 1.5; // 级别加成
            totalScore += applyDecay(baseScore, pr.getStartDate(), config.getDecayRate()) * config.getProjectWeight();
        }

        return Math.round(totalScore * 100.0) / 100.0; // 保留两位小数
    }

    /**
     * 时间衰减函数: W(t) = W0 * e^(-lambda * t)
     * t 为距今年数
     */
    private double applyDecay(double baseScore, LocalDate date, double lambda) {
        if (date == null || lambda == 0) return baseScore;
        int years = Period.between(date, LocalDate.now()).getYears();
        return baseScore * Math.exp(-lambda * years);
    }

    /**
     * 从数据库获取权重配置，不存在时使用默认模板
     */
    private WeightConfig getWeightConfig(String discipline) {
        // 优先从数据库查询学科权重配置
        if (discipline != null && !discipline.isEmpty()) {
            WeightConfig dbConfig = weightConfigRepo.findByDiscipline(discipline);
            if (dbConfig != null) {
                log.info("使用数据库权重配置: discipline={}, paper={}, patent={}, project={}, decay={}",
                        discipline, dbConfig.getPaperWeight(), dbConfig.getPatentWeight(),
                        dbConfig.getProjectWeight(), dbConfig.getDecayRate());
                return dbConfig;
            }
        }

        // 数据库中未找到配置时，使用默认模板
        log.info("未找到学科[{}]的权重配置，使用默认配置", discipline);
        WeightConfig config = new WeightConfig();
        config.setDecayRate(0.05); // 默认衰减系数

        if ("工学".equals(discipline)) {
            config.setPaperWeight(0.3);
            config.setPatentWeight(0.4);
            config.setProjectWeight(0.3);
        } else if ("理学".equals(discipline)) {
            config.setPaperWeight(0.6);
            config.setPatentWeight(0.1);
            config.setProjectWeight(0.3);
        } else {
            // 默认/人文社科
            config.setPaperWeight(0.5);
            config.setPatentWeight(0.1);
            config.setProjectWeight(0.4);
        }
        return config;
    }
}
