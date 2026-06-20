package com.jlvtc.researchportrait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableNeo4jRepositories // 启用 Neo4j
@EnableScheduling        // 启用定时任务
public class ResearchPortraitApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResearchPortraitApplication.class, args);
    }
}