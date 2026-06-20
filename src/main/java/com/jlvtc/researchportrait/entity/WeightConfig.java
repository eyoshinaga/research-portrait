package com.jlvtc.researchportrait.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("WeightConfig")
public class WeightConfig {

    @Id
    private Long id;

    @Property("discipline")
    private String discipline; // 学科门类

    @Property("paperWeight")
    private Double paperWeight; // 论文权重

    @Property("patentWeight")
    private Double patentWeight; // 专利权重

    @Property("projectWeight")
    private Double projectWeight; // 项目权重

    @Property("decayRate")
    private Double decayRate; // 时间衰减系数 lambda

    public WeightConfig() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public Double getPaperWeight() { return paperWeight; }
    public void setPaperWeight(Double paperWeight) { this.paperWeight = paperWeight; }
    public Double getPatentWeight() { return patentWeight; }
    public void setPatentWeight(Double patentWeight) { this.patentWeight = patentWeight; }
    public Double getProjectWeight() { return projectWeight; }
    public void setProjectWeight(Double projectWeight) { this.projectWeight = projectWeight; }
    public Double getDecayRate() { return decayRate; }
    public void setDecayRate(Double decayRate) { this.decayRate = decayRate; }
}
