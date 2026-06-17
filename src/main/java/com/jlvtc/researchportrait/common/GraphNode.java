package com.jlvtc.researchportrait.common;

import java.util.Map;

public class GraphNode {
    private String id;
    private String label;
    private String type;
    private Map<String, Object> properties;

    public GraphNode() {}

    public GraphNode(String id, String label, String type, Map<String, Object> properties) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.properties = properties;
    }

    // getter + setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}