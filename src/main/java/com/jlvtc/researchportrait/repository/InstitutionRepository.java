package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.Institution;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface InstitutionRepository extends Neo4jRepository<Institution, Long> {
}