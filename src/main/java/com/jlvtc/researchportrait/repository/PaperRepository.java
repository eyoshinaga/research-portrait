package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.Paper;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PaperRepository extends Neo4jRepository<Paper, Long> {
}