package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.Patent;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PatentRepository extends Neo4jRepository<Patent, Long> {
}