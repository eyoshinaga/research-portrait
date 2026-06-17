package com.jlvtc.researchportrait.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphResult {
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> links;
}