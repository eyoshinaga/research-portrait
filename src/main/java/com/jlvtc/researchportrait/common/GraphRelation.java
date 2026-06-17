package com.jlvtc.researchportrait.common;

public class GraphRelation {
    private String id;
    private String start;
    private String end;
    private String type;

    public GraphRelation() {}

    public GraphRelation(String id, String start, String end, String type) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.type = type;
    }

    // getter + setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}