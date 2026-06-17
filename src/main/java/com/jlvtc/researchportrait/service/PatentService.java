package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.Patent;
import com.jlvtc.researchportrait.repository.PatentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatentService {
    @Autowired
    private PatentRepository patentRepository;

    public Patent save(Patent patent) {
        return patentRepository.save(patent);
    }

    public List<Patent> getAll() {
        return patentRepository.findAll();
    }
}