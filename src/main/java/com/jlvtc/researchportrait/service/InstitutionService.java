package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.Institution;
import com.jlvtc.researchportrait.repository.InstitutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InstitutionService {
    @Autowired
    private InstitutionRepository institutionRepository;

    public Institution save(Institution institution) {
        return institutionRepository.save(institution);
    }

    public List<Institution> getAll() {
        return institutionRepository.findAll();
    }
}