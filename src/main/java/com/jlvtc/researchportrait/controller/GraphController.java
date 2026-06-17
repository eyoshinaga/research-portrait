package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.common.GraphResult;
import com.jlvtc.researchportrait.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // 获取全量图谱
    @GetMapping("/full")
    public GraphResult getFullGraph() {
        return graphService.getFullGraph();
    }

    // 双击展开邻居（方法名 100% 统一）
    @GetMapping("/expand/{nodeId}")
    public GraphResult expand(@PathVariable long nodeId) {
        return graphService.expandNeighbors(nodeId);
    }
}