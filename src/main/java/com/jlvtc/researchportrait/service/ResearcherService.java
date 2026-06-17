package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 科研人员 业务层
 */
@Service
public class ResearcherService {

    @Autowired
    private ResearcherRepository researcherRepository;
    
    @Autowired
    private PaperRepository paperRepository;
    
    @Autowired
    private PatentRepository patentRepository;
    
    @Autowired
    private ProjectRepository projectRepository;

    /**
     * 保存/更新科研人员
     */
    public Researcher saveResearcher(Researcher researcher) {
        return researcherRepository.save(researcher);
    }

    /**
     * 根据ID查询
     */
    public Researcher findById(Long id) {
        Optional<Researcher> optional = researcherRepository.findById(id);
        return optional.orElse(null);
    }

    /**
     * 查询所有科研人员
     */
    public List<Researcher> findAll() {
        return researcherRepository.findAll();
    }

    /**
     * 删除
     */
    public void deleteById(Long id) {
        researcherRepository.deleteById(id);
    }

    /**
     * 获取科研人员多维画像详情
     */
    public Map<String, Object> getProfileDetail(Long id) {
        Researcher r = findById(id);
        if (r == null) return Collections.emptyMap();

        Map<String, Object> detail = new HashMap<>();
        detail.put("basicInfo", r);
        
        // 1. 产出趋势统计 (按年份)
        Map<Integer, Long> paperTrend = r.getPapers().stream()
            .filter(p -> p.getPubDate() != null)
            .collect(Collectors.groupingBy(p -> p.getPubDate().getYear(), Collectors.counting()));
        
        Map<Integer, Long> patentTrend = r.getPatents().stream()
            .filter(p -> p.getApplyDate() != null)
            .collect(Collectors.groupingBy(p -> p.getApplyDate().getYear(), Collectors.counting()));
            
        detail.put("trends", Map.of("papers", paperTrend, "patents", patentTrend));
        
        // 2. 项目经费统计
        double totalFund = r.getChargeProjects().stream()
            .mapToDouble(p -> p.getFund() != null ? p.getFund() : 0)
            .sum();
        detail.put("totalFunding", totalFund);
        
        // 3. 研究主题演变 (提取论文标题关键词)
        Map<Integer, List<String>> topicEvolution = r.getPapers().stream()
            .filter(p -> p.getPubDate() != null && p.getTitle() != null)
            .collect(Collectors.groupingBy(
                p -> p.getPubDate().getYear(),
                Collectors.mapping(p -> extractKeywords(p.getTitle()), Collectors.toList())
            ));
        detail.put("topicEvolution", topicEvolution);
        
        return detail;
    }

    /**
     * 简单的关键词提取模拟（实际生产应接入 HanLP 或 Jieba）
     */
    private String extractKeywords(String title) {
        // 这里简单按空格或标点分割，并过滤掉常见停用词
        String[] words = title.split("[\\s,，。、]" );
        for (String word : words) {
            if (word.length() > 1) return word; // 返回第一个有意义的词作为示例
        }
        return "综合研究";
    }
}