package com.jlvtc.researchportrait.repository;

import com.jlvtc.researchportrait.entity.WeightConfig;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeightConfigRepository extends Neo4jRepository<WeightConfig, Long> {

    /**
     * 根据学科门类查询权重配置
     */
    WeightConfig findByDiscipline(String discipline);
}
