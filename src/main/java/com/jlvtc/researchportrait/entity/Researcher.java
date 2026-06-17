package com.jlvtc.researchportrait.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Node("Researcher")
public class Researcher {

    @Id
    private Long id;

    @Property("name")
    private String name;

    @Property("age")
    private Integer age;

    @Property("department")
    private String department;

    @Property("title")
    private String title;

    @Property("researchField")
    private String researchField;

    // 隶属机构: BELONG_TO
    @Relationship(type = "BELONG_TO", direction = Relationship.Direction.OUTGOING)
    private Institution institution;

    // 发表论文: WRITE
    @Relationship(type = "WRITE", direction = Relationship.Direction.OUTGOING)
    private List<Paper> papers = new ArrayList<>();

    // 申请专利: INVENT
    @Relationship(type = "INVENT", direction = Relationship.Direction.OUTGOING)
    private List<Patent> patents = new ArrayList<>();

    // 负责项目: CHARGE
    @Relationship(type = "CHARGE", direction = Relationship.Direction.OUTGOING)
    private List<Project> chargeProjects = new ArrayList<>();

    // 参与项目: PARTICIPATE
    @Relationship(type = "PARTICIPATE", direction = Relationship.Direction.OUTGOING)
    private List<Project> joinProjects = new ArrayList<>();

    // 合作学者: COOPERATE_WITH (无向关系)
    @Relationship(type = "COOPERATE_WITH")
    private Set<Researcher> cooperators = new HashSet<>();

    public Researcher() {}

    public Researcher(String name, Integer age, String department, String title, String researchField) {
        this.name = name;
        this.age = age;
        this.department = department;
        this.title = title;
        this.researchField = researchField;
    }

    // 所有 getter & setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResearchField() {
        return researchField;
    }

    public void setResearchField(String researchField) {
        this.researchField = researchField;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Paper> getPapers() {
        return papers;
    }

    public void setPapers(List<Paper> papers) {
        this.papers = papers;
    }

    public List<Patent> getPatents() {
        return patents;
    }

    public void setPatents(List<Patent> patents) {
        this.patents = patents;
    }

    public List<Project> getChargeProjects() {
        return chargeProjects;
    }

    public void setChargeProjects(List<Project> chargeProjects) {
        this.chargeProjects = chargeProjects;
    }

    public List<Project> getJoinProjects() {
        return joinProjects;
    }

    public void setJoinProjects(List<Project> joinProjects) {
        this.joinProjects = joinProjects;
    }

    public Set<Researcher> getCooperators() {
        return cooperators;
    }

    public void setCooperators(Set<Researcher> cooperators) {
        this.cooperators = cooperators;
    }
}