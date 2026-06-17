package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.Paper;
import com.jlvtc.researchportrait.repository.PaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaperService {
    @Autowired
    private PaperRepository paperRepository;

    public Paper save(Paper paper) {
        return paperRepository.save(paper);
    }

    public List<Paper> getAll() {
        return paperRepository.findAll();
    }
}