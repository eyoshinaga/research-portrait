package com.jlvtc.researchportrait.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;

@Node("Project")
public class Project {
    @Id
    private Long id;

    @Property("projName")
    private String projName;

    @Property("fund")
    private Double fund;

    @Property("startDate")
    private LocalDate startDate;

    @Property("endDate")
    private LocalDate endDate;

    @Property("projLevel")
    private String projLevel;

    public Project() {}

    // getter & setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjName() {
        return projName;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public Double getFund() {
        return fund;
    }

    public void setFund(Double fund) {
        this.fund = fund;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getProjLevel() {
        return projLevel;
    }

    public void setProjLevel(String projLevel) {
        this.projLevel = projLevel;
    }
}