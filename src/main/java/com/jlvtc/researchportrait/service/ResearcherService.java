package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.Researcher;
import com.jlvtc.researchportrait.repository.ResearcherRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

/**
 * 科研人员 业务层
 */
@Service
public class ResearcherService {

    // 用@Autowired替代@Resource，Spring Boot 3.x默认更兼容
    @Autowired
    private ResearcherRepository researcherRepository;

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
}