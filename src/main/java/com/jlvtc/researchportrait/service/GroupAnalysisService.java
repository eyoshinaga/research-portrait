package com.jlvtc.researchportrait.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * 科研人员群体对比分析业务层
 * 支持按学科领域、职称级别、所属院系、人才类别四个维度进行群体划分，
 * 并在科研产出、影响力、合作广度等指标上进行聚合对比与年度趋势分析。
 */
@Service
@RequiredArgsConstructor
public class GroupAnalysisService {

    private final Driver driver;

    /** 人才类别派生表达式：领军人才 / 学术骨干 / 青年学者 */
    private static final String TALENT_CASE =
        "CASE " +
        "WHEN r.influenceIndex >= 85 OR (r.title IN ['教授','研究员','特聘研究员'] " +
        "     AND EXISTS { MATCH (r)-[:`CHARGE`]->(p0:Project) WHERE p0.projLevel = '国家级' }) THEN '领军人才' " +
        "WHEN r.influenceIndex >= 65 THEN '学术骨干' " +
        "ELSE '青年学者' END";

    /** 各分析维度对应的分组表达式 */
    private static final Map<String, String> DIMENSION_EXPR = new LinkedHashMap<>();
    static {
        DIMENSION_EXPR.put("discipline", "coalesce(r.disciplineCategory, '未分类')");
        DIMENSION_EXPR.put("title", "coalesce(r.title, '未定级')");
        DIMENSION_EXPR.put("department", "coalesce(r.department, '未分配')");
        DIMENSION_EXPR.put("talentCategory", TALENT_CASE);
    }

    /** 群体聚合指标查询模板（%s 为分组表达式） */
    private static final String AGG_CYPHER_TEMPLATE =
        "MATCH (r:Researcher) " +
        "WITH r, %s AS grp " +
        "OPTIONAL MATCH (r)-[:WRITE]->(p:Paper) " +
        "OPTIONAL MATCH (r)-[:INVENT]->(pat:Patent) " +
        "OPTIONAL MATCH (r)-[:`CHARGE`|PARTICIPATE]->(proj:Project) " +
        "OPTIONAL MATCH (r)-[:COOPERATE_WITH]-(c:Researcher) " +
        "WITH grp, r, collect(DISTINCT p) AS papers, collect(DISTINCT pat) AS patents, " +
        "     collect(DISTINCT proj) AS projects, count(DISTINCT c) AS coopCnt " +
        "RETURN grp AS groupName, " +
        "       count(r) AS cnt, " +
        "       sum(size(papers)) AS paperCount, " +
        "       sum(size(patents)) AS patentCount, " +
        "       sum(size(projects)) AS projectCount, " +
        "       sum(size([x IN papers WHERE x.international = true])) AS intlPaperCount, " +
        "       sum(size([x IN patents WHERE x.transferred = true])) AS transferredPatentCount, " +
        "       sum(reduce(f = 0.0, x IN projects | f + coalesce(x.fund, 0.0))) AS fundSum, " +
        "       avg(r.influenceIndex) AS avgInfluence, " +
        "       avg(coopCnt) AS avgCooperators " +
        "ORDER BY cnt DESC";

    /** 群体年度论文产出趋势查询模板（WITH 中需保留 p 供后续去重计数引用） */
    private static final String YEAR_CYPHER_TEMPLATE =
        "MATCH (r:Researcher)-[:WRITE]->(p:Paper) " +
        "WHERE p.pubDate IS NOT NULL " +
        "WITH %s AS grp, p.pubDate.year AS y, p " +
        "RETURN grp AS groupName, y, count(DISTINCT p) AS cnt " +
        "ORDER BY groupName, y";

    /**
     * 多维度群体对比分析
     * @param dimension discipline(学科领域) / title(职称级别) / department(所属院系) / talentCategory(人才类别)
     */
    public Map<String, Object> groupCompare(String dimension) {
        String dim = DIMENSION_EXPR.containsKey(dimension) ? dimension : "discipline";
        String expr = DIMENSION_EXPR.get(dim);

        List<Map<String, Object>> groups = new ArrayList<>();
        Map<String, Map<String, Object>> groupIndex = new LinkedHashMap<>();

        try (Session session = driver.session()) {
            Result res = session.run(String.format(AGG_CYPHER_TEMPLATE, expr));
            while (res.hasNext()) {
                Record rec = res.next();
                String name = rec.get("groupName").isNull() ? "未分类" : rec.get("groupName").asString();
                long cnt = rec.get("cnt").asLong();
                long paperCount = rec.get("paperCount").asLong();
                long patentCount = rec.get("patentCount").asLong();
                long projectCount = rec.get("projectCount").asLong();

                Map<String, Object> g = new LinkedHashMap<>();
                g.put("groupName", name);
                g.put("count", cnt);
                g.put("paperCount", paperCount);
                g.put("patentCount", patentCount);
                g.put("projectCount", projectCount);
                g.put("fundSum", round2(rec.get("fundSum").isNull() ? 0 : rec.get("fundSum").asDouble()));
                g.put("avgInfluence", round2(rec.get("avgInfluence").isNull() ? 0 : rec.get("avgInfluence").asDouble()));
                g.put("avgPapers", cnt > 0 ? round2(paperCount * 1.0 / cnt) : 0);
                g.put("avgPatents", cnt > 0 ? round2(patentCount * 1.0 / cnt) : 0);
                g.put("avgProjects", cnt > 0 ? round2(projectCount * 1.0 / cnt) : 0);
                g.put("avgCooperators", round2(rec.get("avgCooperators").isNull() ? 0 : rec.get("avgCooperators").asDouble()));
                g.put("intlPaperCount", rec.get("intlPaperCount").asLong());
                g.put("transferredPatentCount", rec.get("transferredPatentCount").asLong());
                g.put("byYear", new LinkedHashMap<String, Long>());
                groups.add(g);
                groupIndex.put(name, g);
            }

            // 年度趋势：按组 × 年份统计论文产出
            Result yearRes = session.run(String.format(YEAR_CYPHER_TEMPLATE, expr));
            while (yearRes.hasNext()) {
                Record rec = yearRes.next();
                String name = rec.get("groupName").isNull() ? "未分类" : rec.get("groupName").asString();
                Map<String, Object> g = groupIndex.get(name);
                if (g == null) continue;
                @SuppressWarnings("unchecked")
                Map<String, Long> byYear = (Map<String, Long>) g.get("byYear");
                byYear.put(String.valueOf(rec.get("y").asInt()), rec.get("cnt").asLong());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimension", dim);
        result.put("groups", groups);
        return result;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
