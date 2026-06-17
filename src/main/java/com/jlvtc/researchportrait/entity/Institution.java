package com.jlvtc.researchportrait.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Institution")
public class Institution {
    @Id
    private Long id;

    @Property("instName")
    private String instName;

    @Property("type")
    private String type;

    public Institution() {}

    public Institution(String instName, String type) {
        this.instName = instName;
        this.type = type;
    }

    // getter & setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstName() {
        return instName;
    }

    public void setInstName(String instName) {
        this.instName = instName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}