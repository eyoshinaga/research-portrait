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
import java.util.stream.Collectors;

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
    // 获取采样图谱（初始页面加载用，随机抽取部分节点）
    // ============================
    public GraphResult getSampleGraph(int researcherLimit) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Set<Long> nodeIdSet = new HashSet<>();

        try (Session session = driver.session()) {
            // 1. 随机抽取一部分科研人员
            Result sampleResult = session.run(
                "MATCH (r:Researcher) RETURN r, id(r) AS nid ORDER BY rand() LIMIT $limit",
                Map.of("limit", researcherLimit)
            );
            Set<Long> sampledIds = new HashSet<>();
            while (sampleResult.hasNext()) {
                Record rec = sampleResult.next();
                long nid = rec.get("nid").asLong();
                nodeIdSet.add(nid);
                sampledIds.add(nid);
                nodes.add(nodeToMap(rec.get("r").asNode(), nid));
            }

            // 2. 查询这些科研人员之间的关系
            Result relResult = session.run(
                "MATCH (a)-[r]->(b) WHERE id(a) IN $ids AND id(b) IN $ids " +
                "RETURN r, id(r) AS rid, id(a) AS sid, id(b) AS tid",
                Map.of("ids", new ArrayList<>(sampledIds))
            );
            while (relResult.hasNext()) {
                Record rec = relResult.next();
                Map<String, Object> relMap = new HashMap<>();
                relMap.put("id", rec.get("rid").asLong());
                relMap.put("source", rec.get("sid").asLong());
                relMap.put("target", rec.get("tid").asLong());
                relMap.put("type", rec.get("r").asRelationship().type());
                links.add(relMap);
            }

            // 3. 查询这些科研人员所属院系（连带显示）
            Result instResult = session.run(
                "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution) WHERE id(r) IN $ids " +
                "RETURN i, id(i) AS iid, id(r) AS rid",
                Map.of("ids", new ArrayList<>(sampledIds))
            );
            while (instResult.hasNext()) {
                Record rec = instResult.next();
                long iid = rec.get("iid").asLong();
                if (!nodeIdSet.contains(iid)) {
                    nodeIdSet.add(iid);
                    nodes.add(nodeToMap(rec.get("i").asNode(), iid));
                }
                // 添加 BELONG_TO 关系
                Map<String, Object> relMap = new HashMap<>();
                relMap.put("id", -1L); // 虚拟ID
                relMap.put("source", rec.get("rid").asLong());
                relMap.put("target", iid);
                relMap.put("type", "BELONG_TO");
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
    // 二级合作网络探查（中心科研人员 → 直接合作者 → 延伸出的二级合作者）
    // ============================
    /**
     * 以指定科研人员为中心探查合作网络：
     * depth=1：中心 + 直接合作者（一级）
     * depth=2：中心 + 直接合作者 + 通过直接合作者延伸出的二级合作者
     */
    public Map<String, Object> getCooperationNetwork(Long researcherId, int depth) {
        Map<String, Object> result = new HashMap<>();
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Set<Long> level1Ids = new HashSet<>();

        try (Session session = driver.session()) {
            // 0. 中心节点
            Result centerRes = session.run(
                    "MATCH (r:Researcher {id: $rid}) RETURN r, id(r) AS nid",
                    Map.of("rid", researcherId));
            if (!centerRes.hasNext()) {
                return result;
            }
            Record centerRec = centerRes.next();
            long centerId = centerRec.get("nid").asLong();
            Map<String, Object> centerNode = nodeToMap(centerRec.get("r").asNode(), centerId);
            centerNode.put("coopLevel", 0);
            nodeMap.put(centerId, centerNode);

            // 1. 一级：直接合作者（COOPERATE_WITH 无向关系）
            Result l1Res = session.run(
                    "MATCH (r:Researcher {id: $rid})-[rel:COOPERATE_WITH]-(c1:Researcher) " +
                            "RETURN c1, id(c1) AS cid, id(rel) AS rid, id(startNode(rel)) AS sid, id(endNode(rel)) AS tid",
                    Map.of("rid", researcherId));
            while (l1Res.hasNext()) {
                Record rec = l1Res.next();
                long cid = rec.get("cid").asLong();
                level1Ids.add(cid);
                if (!nodeMap.containsKey(cid)) {
                    Map<String, Object> n = nodeToMap(rec.get("c1").asNode(), cid);
                    n.put("coopLevel", 1);
                    nodeMap.put(cid, n);
                }
                Map<String, Object> relMap = new HashMap<>();
                relMap.put("id", rec.get("rid").asLong());
                relMap.put("source", rec.get("sid").asLong());
                relMap.put("target", rec.get("tid").asLong());
                relMap.put("type", "COOPERATE_WITH");
                relMap.put("level", 1);
                links.add(relMap);
            }

            // 2. 二级：通过直接合作者延伸出的二级合作者（排除中心自身与直接合作者）
            if (depth >= 2) {
                Result l2Res = session.run(
                        "MATCH (r:Researcher {id: $rid})-[:COOPERATE_WITH]-(c1:Researcher)-[rel2:COOPERATE_WITH]-(c2:Researcher) " +
                                "WHERE c2 <> r AND NOT (r)-[:COOPERATE_WITH]-(c2) " +
                                "RETURN DISTINCT c2, id(c2) AS cid, id(rel2) AS rid, id(startNode(rel2)) AS sid, id(endNode(rel2)) AS tid",
                        Map.of("rid", researcherId));
                while (l2Res.hasNext()) {
                    Record rec = l2Res.next();
                    long cid = rec.get("cid").asLong();
                    if (!nodeMap.containsKey(cid)) {
                        Map<String, Object> n = nodeToMap(rec.get("c2").asNode(), cid);
                        n.put("coopLevel", 2);
                        nodeMap.put(cid, n);
                    }
                    Map<String, Object> relMap = new HashMap<>();
                    relMap.put("id", rec.get("rid").asLong());
                    relMap.put("source", rec.get("sid").asLong());
                    relMap.put("target", rec.get("tid").asLong());
                    relMap.put("type", "COOPERATE_WITH");
                    relMap.put("level", 2);
                    links.add(relMap);
                }
            }
        }

        // 3. 全图分析：为探查网络中的节点标注枢纽/社群信息并返回全局枢纽与社群列表
        NetworkMetrics metrics = computeNetworkMetrics();
        for (Map<String, Object> node : nodeMap.values()) {
            Object nidObj = node.get("id");
            if (!(nidObj instanceof Long)) continue;
            Long nid = (Long) nidObj;
            node.put("degree", metrics.degree.getOrDefault(nid, 0));
            node.put("hubScore", metrics.hubScore.getOrDefault(nid, 0));
            node.put("isHub", metrics.hubs.contains(nid));
            node.put("communityId", metrics.communityOf.getOrDefault(nid, 0));
        }

        long level2Count = nodeMap.values().stream()
                .filter(n -> Integer.valueOf(2).equals(n.get("coopLevel")))
                .count();
        result.put("center", nodeMap.isEmpty() ? null : nodeMap.get(nodeMap.keySet().iterator().next()));
        result.put("nodes", new ArrayList<>(nodeMap.values()));
        result.put("links", links);
        result.put("level1Count", level1Ids.size());
        result.put("level2Count", level2Count);
        result.put("depth", depth);
        result.put("hubs", metrics.hubList);
        result.put("communities", metrics.communityList);
        result.put("communityCount", metrics.communityList.size());
        return result;
    }

    // ============================
    // 全图合作网络分析：枢纽识别 + 社群检测
    // ============================
    /**
     * 全图合作网络分析：
     * 1. 度数中心度 → 识别核心枢纽学者（度数不低于平均值 1.5 倍，最多取 20 位）
     * 2. 标签传播算法（LPA）→ 检测紧密合作的学术社群/团队
     * 使用固定随机种子，保证相同数据下结果稳定。
     */
    private NetworkMetrics computeNetworkMetrics() {
        NetworkMetrics metrics = new NetworkMetrics();
        Map<Long, Map<String, Object>> nodeInfo = new LinkedHashMap<>();
        Map<Long, Set<Long>> adj = new HashMap<>();

        try (Session session = driver.session()) {
            // 加载全部科研人员节点
            Result res = session.run("MATCH (r:Researcher) RETURN r, id(r) AS nid");
            while (res.hasNext()) {
                Record rec = res.next();
                long nid = rec.get("nid").asLong();
                nodeInfo.put(nid, nodeToMap(rec.get("r").asNode(), nid));
                adj.put(nid, new HashSet<>());
            }

            // 加载全部合作关系（无向化）
            Result relRes = session.run(
                    "MATCH (a:Researcher)-[:COOPERATE_WITH]->(b:Researcher) " +
                    "RETURN id(a) AS aid, id(b) AS bid");
            while (relRes.hasNext()) {
                Record rec = relRes.next();
                long aid = rec.get("aid").asLong();
                long bid = rec.get("bid").asLong();
                if (aid == bid || !adj.containsKey(aid) || !adj.containsKey(bid)) continue;
                adj.get(aid).add(bid);
                adj.get(bid).add(aid);
            }
        }

        // 1. 度数中心度 → 核心枢纽学者
        int maxDegree = 0;
        for (Map.Entry<Long, Set<Long>> e : adj.entrySet()) {
            int d = e.getValue().size();
            metrics.degree.put(e.getKey(), d);
            if (d > maxDegree) maxDegree = d;
        }
        double avgDegree = metrics.degree.values().stream()
                .mapToInt(Integer::intValue).average().orElse(0);
        double hubThreshold = Math.max(avgDegree * 1.5, 8);

        List<Long> hubIds = metrics.degree.entrySet().stream()
                .filter(e -> e.getValue() >= hubThreshold)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (Long nid : hubIds) {
            metrics.hubs.add(nid);
            int score = maxDegree > 0
                    ? (int) Math.round(metrics.degree.get(nid) * 100.0 / maxDegree) : 0;
            metrics.hubScore.put(nid, score);
            Map<String, Object> props = nodeProps(nodeInfo.get(nid));
            Map<String, Object> hub = new LinkedHashMap<>();
            hub.put("id", nid);
            hub.put("name", props.get("name"));
            hub.put("department", props.get("department"));
            hub.put("title", props.get("title"));
            hub.put("degree", metrics.degree.get(nid));
            hub.put("hubScore", score);
            metrics.hubList.add(hub);
        }

        // 2. LPA 社群检测
        metrics.communityOf.putAll(detectCommunities(adj));

        Map<Integer, List<Long>> members = new TreeMap<>();
        for (Map.Entry<Long, Integer> e : metrics.communityOf.entrySet()) {
            members.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<Integer, List<Long>> e : members.entrySet()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("communityId", e.getKey());
            c.put("size", e.getValue().size());
            // 社群代表成员（社群内度数最高的前 3 位）
            List<Map<String, Object>> topMembers = e.getValue().stream()
                    .sorted((a, b) -> metrics.degree.getOrDefault(b, 0) - metrics.degree.getOrDefault(a, 0))
                    .limit(3)
                    .map(nid -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", nid);
                        m.put("name", nodeProps(nodeInfo.get(nid)).get("name"));
                        m.put("degree", metrics.degree.getOrDefault(nid, 0));
                        return m;
                    })
                    .collect(Collectors.toList());
            c.put("topMembers", topMembers);
            c.put("hubCount", (int) e.getValue().stream().filter(metrics.hubs::contains).count());
            metrics.communityList.add(c);
        }

        return metrics;
    }

    /** 安全提取节点属性 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nodeProps(Map<String, Object> node) {
        if (node == null) return Collections.emptyMap();
        Object props = node.get("props");
        return props instanceof Map ? (Map<String, Object>) props : Collections.emptyMap();
    }

    /**
     * 标签传播算法（LPA）社群检测：
     * 初始每个节点持有唯一标签，迭代将标签更新为邻居中出现最多的标签，直至收敛；
     * 使用固定随机种子保证结果确定性；社群编号按规模降序规范为 1..N。
     */
    private Map<Long, Integer> detectCommunities(Map<Long, Set<Long>> adj) {
        Map<Long, Long> labels = new HashMap<>();
        for (Long nid : adj.keySet()) {
            labels.put(nid, nid);
        }

        List<Long> nodes = new ArrayList<>(adj.keySet());
        Random random = new Random(42);
        for (int iter = 0; iter < 30; iter++) {
            Collections.shuffle(nodes, random);
            int changed = 0;
            for (Long nid : nodes) {
                Set<Long> neighbors = adj.get(nid);
                if (neighbors.isEmpty()) continue;
                Map<Long, Integer> freq = new HashMap<>();
                for (Long nb : neighbors) {
                    freq.merge(labels.get(nb), 1, Integer::sum);
                }
                Long best = null;
                int bestCnt = -1;
                for (Map.Entry<Long, Integer> e : freq.entrySet()) {
                    if (e.getValue() > bestCnt || (e.getValue() == bestCnt && (best == null || e.getKey() < best))) {
                        best = e.getKey();
                        bestCnt = e.getValue();
                    }
                }
                if (best != null && !best.equals(labels.get(nid))) {
                    labels.put(nid, best);
                    changed++;
                }
            }
            if (changed == 0) break;
        }

        // 社群编号规范化：按规模降序编号 1..N
        Map<Long, Integer> counts = new HashMap<>();
        for (Long l : labels.values()) {
            counts.merge(l, 1, Integer::sum);
        }
        List<Long> sorted = new ArrayList<>(counts.keySet());
        sorted.sort((a, b) -> {
            int cmp = counts.get(b) - counts.get(a);
            return cmp != 0 ? cmp : Long.compare(a, b);
        });
        Map<Long, Integer> labelToCommunity = new HashMap<>();
        int cid = 1;
        for (Long l : sorted) {
            labelToCommunity.put(l, cid++);
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<Long, Long> e : labels.entrySet()) {
            result.put(e.getKey(), labelToCommunity.get(e.getValue()));
        }
        return result;
    }

    /** 合作网络全图分析结果（枢纽 + 社群） */
    private static class NetworkMetrics {
        final Map<Long, Integer> degree = new HashMap<>();
        final Map<Long, Integer> hubScore = new HashMap<>();
        final Set<Long> hubs = new HashSet<>();
        final Map<Long, Integer> communityOf = new HashMap<>();
        final List<Map<String, Object>> hubList = new ArrayList<>();
        final List<Map<String, Object>> communityList = new ArrayList<>();
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

        map.put("props", new HashMap<>(node.asMap()));
        return map;
    }
}