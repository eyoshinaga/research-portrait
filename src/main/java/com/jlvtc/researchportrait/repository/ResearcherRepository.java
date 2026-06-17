package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.Researcher;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * 科研人员 Repository
 * Neo4jRepository<实体类, ID类型>
 */
@Repository
public interface ResearcherRepository extends Neo4jRepository<Researcher, Long> {

    // 先不用写方法，继承自带：
    // save()、findById()、findAll()、delete() 等
}