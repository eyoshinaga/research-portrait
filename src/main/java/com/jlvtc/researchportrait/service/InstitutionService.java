package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepo;
    private final ResearcherRepository researcherRepo;
    private final Driver driver;

    /**
     * 单机构竞争力雷达图 Cypher 查询
     */
    private static final String RADAR_CYPHER =
        "MATCH (i:Institution {id: $instId}) " +
        "MATCH (r:Researcher)-[:BELONG_TO]->(i) " +
        "WITH i, collect(r) AS researchers " +
        "UNWIND researchers AS r " +
        "OPTIONAL MATCH (r)-[:WRITE]->(p:Paper) " +
        "OPTIONAL MATCH (r)-[:INVENT]->(pat:Patent) " +
        "OPTIONAL MATCH (r)-[:`CHARGE`]->(proj1:Project) " +
        "OPTIONAL MATCH (r)-[:PARTICIPATE]->(proj2:Project) " +
        "WITH i, researchers, " +
        "     count(DISTINCT r) AS researcherCount, " +
        "     count(DISTINCT p) AS paperCount, " +
        "     count(DISTINCT pat) AS patentCount, " +
        "     count(DISTINCT proj1) + count(DISTINCT proj2) AS projectCount, " +
        "     avg(p.impactFactor) AS avgIf, " +
        "     count(DISTINCT CASE WHEN proj1.projLevel = '国家级' THEN proj1 END) AS nationalProjects, " +
        "     avg(r.influenceIndex) AS avgInfluence, " +
        "     count(DISTINCT CASE WHEN pat.patentType = '发明专利' THEN pat END) AS inventionCount " +
        "RETURN coalesce(i.instName, i.name, '未知机构') AS instName, " +
        "       i.type AS instType, " +
        "       researcherCount, paperCount, patentCount, projectCount, " +
        "       avgIf, nationalProjects, avgInfluence, inventionCount";

    /**
     * 多机构竞争力对比 Cypher 查询（UNWIND 一括処理）
     */
    private static final String COMPARE_CYPHER =
        "UNWIND $instIds AS instId " +
        "MATCH (i:Institution {id: instId}) " +
        "MATCH (r:Researcher)-[:BELONG_TO]->(i) " +
        "WITH i, instId, collect(r) AS researchers " +
        "UNWIND researchers AS r " +
        "OPTIONAL MATCH (r)-[:WRITE]->(p:Paper) " +
        "OPTIONAL MATCH (r)-[:INVENT]->(pat:Patent) " +
        "OPTIONAL MATCH (r)-[:`CHARGE`]->(proj1:Project) " +
        "OPTIONAL MATCH (r)-[:PARTICIPATE]->(proj2:Project) " +
        "WITH i, instId, researchers, " +
        "     count(DISTINCT r) AS researcherCount, " +
        "     count(DISTINCT p) AS paperCount, " +
        "     count(DISTINCT pat) AS patentCount, " +
        "     count(DISTINCT proj1) + count(DISTINCT proj2) AS projectCount, " +
        "     avg(p.impactFactor) AS avgIf, " +
        "     count(DISTINCT CASE WHEN proj1.projLevel = '国家级' THEN proj1 END) AS nationalProjects, " +
        "     avg(r.influenceIndex) AS avgInfluence, " +
        "     count(DISTINCT CASE WHEN pat.patentType = '发明专利' THEN pat END) AS inventionCount " +
        "RETURN instId, " +
        "       coalesce(i.instName, i.name, '未知机构') AS instName, " +
        "       i.type AS instType, " +
        "       researcherCount, paperCount, patentCount, projectCount, " +
        "       avgIf, nationalProjects, avgInfluence, inventionCount";

    /**
     * 获取指定机构的竞争力雷达图数据 (6维度) — 优化版：单次Cypher查询
     */
    public Map<String, Object> getCompetitivenessRadar(Long instId) {
        try (Session session = driver.session()) {
            Result result = session.run(RADAR_CYPHER, Map.of("instId", instId));
            if (!result.hasNext()) {
                // 機関が存在しない、または研究者が0名の場合
                Institution inst = institutionRepo.findById(instId).orElse(null);
                if (inst == null) return Collections.emptyMap();
                String instName = inst.getInstName() != null ? inst.getInstName() :
                                  (inst.getName() != null ? inst.getName() : "未知机构");
                String instType = inst.getType() != null ? inst.getType() : "";
                return buildRadarResult(instName, instType, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            Record rec = result.next();
            return buildRadarResult(
                rec.get("instName").asString(),
                rec.get("instType").isNull() ? "" : rec.get("instType").asString(),
                rec.get("researcherCount").asInt(),
                rec.get("paperCount").asLong(),
                rec.get("patentCount").asLong(),
                rec.get("projectCount").asLong(),
                rec.get("avgIf").isNull() ? 0.0 : rec.get("avgIf").asDouble(),
                rec.get("nationalProjects").asLong(),
                rec.get("avgInfluence").isNull() ? 0.0 : rec.get("avgInfluence").asDouble(),
                rec.get("inventionCount").asLong());
        }
    }

    /**
     * 多机构竞争力对比 — 优化版：单次Cypher查询，UNWIND批量处理
     */
    public List<Map<String, Object>> compareInstitutions(List<Long> instIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(COMPARE_CYPHER, Map.of("instIds", instIds));
            while (result.hasNext()) {
                Record rec = result.next();
                results.add(buildRadarResult(
                    rec.get("instName").asString(),
                    rec.get("instType").isNull() ? "" : rec.get("instType").asString(),
                    rec.get("researcherCount").asInt(),
                    rec.get("paperCount").asLong(),
                    rec.get("patentCount").asLong(),
                    rec.get("projectCount").asLong(),
                    rec.get("avgIf").isNull() ? 0.0 : rec.get("avgIf").asDouble(),
                    rec.get("nationalProjects").asLong(),
                    rec.get("avgInfluence").isNull() ? 0.0 : rec.get("avgInfluence").asDouble(),
                    rec.get("inventionCount").asLong()));
            }
        }
        return results;
    }

    /**
     * 根据原始统计数据构建6轴雷达图结果
     */
    private Map<String, Object> buildRadarResult(
            String instName, String instType,
            int researcherCount, long paperCount, long patentCount, long projectCount,
            double avgIf, long nationalProjects, double avgInfluence, long inventionCount) {

        // 1. 科研规模（人员数 + 成果数）
        double scale = researcherCount + paperCount + patentCount + projectCount;

        // 2. 科研质量（平均IF + 国家级项目加分）
        double quality = Math.min((avgIf * 10) + (nationalProjects * 5), 100);

        // 3. 学术影响力（平均影响力指数）
        double influence = avgInfluence;

        // 4. 科研效率（人均成果）
        double efficiency = researcherCount > 0
            ? (paperCount + patentCount) / (double) researcherCount : 0;

        // 5. 学术合作 (根据校内部门类型评估合作活跃度)
        double cooperation;
        if (instType.contains("学院")) {
            cooperation = 75.0;  // 学院规模大、学科交叉多，合作机会丰富
        } else if (instType.contains("重点实验室")) {
            cooperation = 70.0;  // 重点实验室通常承担重大课题，合作活跃
        } else if (instType.contains("研究中心")) {
            cooperation = 65.0;  // 研究中心作为跨学科平台，合作较多
        } else if (instType.contains("系")) {
            cooperation = 60.0;  // 系有一定合作，但范围相对集中
        } else if (instType.contains("研究所")) {
            cooperation = 55.0;  // 研究所合作相对聚焦
        } else {
            cooperation = 40.0;  // 教学部等其他部门合作较少
        }

        // 6. 成果转化（发明专利数 * 权重）
        double conversion = Math.min(inventionCount * 5.0 + (patentCount - inventionCount) * 2.0, 100);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("institutionName", instName);
        result.put("researcherCount", researcherCount);
        result.put("dimensions", new LinkedHashMap<String, Double>() {{
            put("科研规模",    Math.min(scale / 10.0, 100));
            put("科研质量",    quality);
            put("学术影响力", Math.min(influence * 10.0, 100));
            put("科研效率",    Math.min(efficiency * 5.0, 100));
            put("学术合作",    cooperation);
            put("成果转化",    conversion);
        }});
        return result;
    }

    /**
     * 查询所有机构列表（简化字段）
     */
    public List<Map<String, Object>> findAllSimple() {
        return institutionRepo.findAll().stream().map(inst -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", inst.getId());
            map.put("name", inst.getInstName() != null ? inst.getInstName() :
                           (inst.getName() != null ? inst.getName() : "未知机构"));
            return map;
        }).collect(Collectors.toList());
    }
}
