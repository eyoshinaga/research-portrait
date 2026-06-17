package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.common.GraphResult;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final Driver driver;

    // ============================
    // 获取全量知识图谱（标准稳定版）
    // ============================
    public GraphResult getFullGraph() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Set<Long> nodeIdSet = new HashSet<>();

        try (Session session = driver.session()) {

            // 查询所有节点
            Result nodeResult = session.run("MATCH (n) RETURN n, id(n) AS nid");
            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                Node node = record.get("n").asNode();
                long nid = record.get("nid").asLong();

                if (!nodeIdSet.contains(nid)) {
                    nodeIdSet.add(nid);
                    nodes.add(nodeToMap(node, nid));
                }
            }

            // 查询所有关系
            Result relResult = session.run("MATCH (a)-[r]->(b) RETURN r, id(r) AS rid, id(a) AS sid, id(b) AS tid");
            while (relResult.hasNext()) {
                Record record = relResult.next();

                Map<String, Object> relMap = new HashMap<>();
                relMap.put("id", record.get("rid").asLong());
                relMap.put("source", record.get("sid").asLong());
                relMap.put("target", record.get("tid").asLong());
                relMap.put("type", record.get("r").asRelationship().type());
                links.add(relMap);
            }
        }

        return new GraphResult(nodes, links);
    }

    // ============================
    // 双击展开邻居节点（新增功能）
    // ============================
    public GraphResult expandNeighbors(long nodeId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Set<Long> nodeIds = new HashSet<>();

        try (Session session = driver.session()) {
            // 查询当前节点 + 所有邻居
            Result res = session.run(
                    "MATCH (a)-[r]-(b) WHERE id(a) = $id " +
                            "RETURN a, b, r, id(a) AS aid, id(b) AS bid, id(r) AS rid",
                    Map.of("id", nodeId)
            );

            while (res.hasNext()) {
                Record rec = res.next();

                // 处理当前节点 a
                Node a = rec.get("a").asNode();
                long aid = rec.get("aid").asLong();
                if (!nodeIds.contains(aid)) {
                    nodeIds.add(aid);
                    nodes.add(nodeToMap(a, aid));
                }

                // 处理邻居节点 b
                Node b = rec.get("b").asNode();
                long bid = rec.get("bid").asLong();
                if (!nodeIds.contains(bid)) {
                    nodeIds.add(bid);
                    nodes.add(nodeToMap(b, bid));
                }

                // 处理关系
                Relationship r = rec.get("r").asRelationship();
                Map<String, Object> relMap = new HashMap<>();
                relMap.put("id", rec.get("rid").asLong());
                relMap.put("source", aid);
                relMap.put("target", bid);
                relMap.put("type", r.type());
                links.add(relMap);
            }
        }

        return new GraphResult(nodes, links);
    }

    // ============================
    // 工具方法：Node 转 Map（无任何编译错误）
    // ============================
    private Map<String, Object> nodeToMap(Node node, long nid) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", nid);

        // 安全处理标签：Iterable → 手动循环加入 List
        List<String> labels = new ArrayList<>();
        for (String label : node.labels()) {
            labels.add(label);
        }
        map.put("labels", labels);

        map.put("props", node.asMap());
        return map;
    }
}