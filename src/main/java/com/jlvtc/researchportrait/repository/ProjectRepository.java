package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.Project;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ProjectRepository extends Neo4jRepository<Project, Long> {
}