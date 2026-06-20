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
    // 科研合作网络深度分析
    // ============================
    public Map<String, Object> getCooperationAnalysis(Long researcherId) {
        Map<String, Object> result = new HashMap<>();
        try (Session session = driver.session()) {
            // 1. 核心合作者 (Top 5)
            String topCollabQuery = "MATCH (r:Researcher {id: $rid})-[:WRITE|INVENT|PARTICIPATE|CHARGE]-(o)-[:WRITE|INVENT|PARTICIPATE|CHARGE]-(c:Researcher) " +
                    "WHERE r <> c RETURN c, count(o) as freq ORDER BY freq DESC LIMIT 5";
            Result topRes = session.run(topCollabQuery, Map.of("rid", researcherId));
            List<Map<String, Object>> topCollaborators = new ArrayList<>();
            while (topRes.hasNext()) {
                Record rec = topRes.next();
                Node c = rec.get("c").asNode();
                Map<String, Object> collab = new HashMap<>(c.asMap());
                collab.put("frequency", rec.get("freq").asInt());
                topCollaborators.add(collab);
            }
            result.put("topCollaborators", topCollaborators);

            // 2. 跨学科合作度
            String disciplineQuery = "MATCH (r:Researcher {id: $rid})-[:WRITE|INVENT|PARTICIPATE|CHARGE]-(o)-[:WRITE|INVENT|PARTICIPATE|CHARGE]-(c:Researcher) " +
                    "WHERE r <> c AND r.disciplineCategory <> c.disciplineCategory " +
                    "RETURN c.disciplineCategory as discipline, count(c) as count";
            Result discRes = session.run(disciplineQuery, Map.of("rid", researcherId));
            Map<String, Integer> crossDiscipline = new HashMap<>();
            int totalCross = 0;
            while (discRes.hasNext()) {
                Record rec = discRes.next();
                String disc = rec.get("discipline").asString();
                int count = rec.get("count").asInt();
                crossDiscipline.put(disc, count);
                totalCross += count;
            }
            result.put("crossDisciplineStats", crossDiscipline);
            result.put("totalCrossDisciplineCount", totalCross);

            // 3. 合作稳定性 (基于年份)
            String stabilityQuery = "MATCH (r:Researcher {id: $rid})-[:WRITE|INVENT|PARTICIPATE|CHARGE]-(o) " +
                    "WHERE o.pubDate IS NOT NULL OR o.applyDate IS NOT NULL OR o.startDate IS NOT NULL " +
                    "WITH coalesce(o.pubDate, o.applyDate, o.startDate) as year, count(o) as cnt " +
                    "RETURN year.year as y, cnt ORDER BY y";
            Result stabRes = session.run(stabilityQuery, Map.of("rid", researcherId));
            Map<Integer, Integer> yearlyCollab = new LinkedHashMap<>();
            while (stabRes.hasNext()) {
                Record rec = stabRes.next();
                yearlyCollab.put(rec.get("y").asInt(), rec.get("cnt").asInt());
            }
            result.put("yearlyCollaborationTrend", yearlyCollab);
        }
        return result;
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