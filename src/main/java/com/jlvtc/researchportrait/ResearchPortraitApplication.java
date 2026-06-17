package com.jlvtc.researchportrait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EnableNeo4jRepositories // 加上这一行，启用Neo4j
public class ResearchPortraitApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResearchPortraitApplication.class, args);
    }
}