package com.jlvtc.researchportrait.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;

@Node("Paper")
public class Paper {
    @Id
    private Long id;

    @Property("title")
    private String title;

    @Property("journal")
    private String journal;

    @Property("doi")
    private String doi;

    @Property("pubDate")
    private LocalDate pubDate;

    @Property("citedNum")
    private Integer citedNum;

    @Property("subjectField")
    private String subjectField;

    public Paper() {}

    // getter & setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public LocalDate getPubDate() {
        return pubDate;
    }

    public void setPubDate(LocalDate pubDate) {
        this.pubDate = pubDate;
    }

    public Integer getCitedNum() {
        return citedNum;
    }

    public void setCitedNum(Integer citedNum) {
        this.citedNum = citedNum;
    }

    public String getSubjectField() {
        return subjectField;
    }

    public void setSubjectField(String subjectField) {
        this.subjectField = subjectField;
    }
}