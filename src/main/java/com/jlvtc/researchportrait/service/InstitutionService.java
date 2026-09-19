package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepo;
    private final ResearcherRepository researcherRepo;
    private final Driver driver;

    /** 7 维雷达图维度（顺序即雷达图顺时针顺序） */
    public static final String[] DIMENSIONS = {
        "科研规模", "科研质量", "学术影响力", "科研效率", "学术合作", "国际合作", "成果转化"
    };

    /**
     * 多机构竞争力对比 Cypher（UNWIND 批量处理 + CALL 子查询逐项聚合，避免笛卡尔膨胀导致统计失真）
     * 统计口径：
     * - 论文/专利/项目均按去重后的成果实体计数
     * - 平均影响因子在去重后的论文集合上求平均
     * - 合作指标基于 COOPERATE_WITH 无向关系：人均合作者数、跨机构合作占比
     */
    private static final String COMPARE_CYPHER =
        "UNWIND $instIds AS instId " +
        "MATCH (i:Institution {id: instId}) " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) " +
        "  RETURN count(r) AS researcherCount, avg(r.influenceIndex) AS avgInfluence } " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) MATCH (r)-[:WRITE]->(p:Paper) " +
        "  WITH DISTINCT p " +
        "  RETURN count(p) AS paperCount, avg(p.impactFactor) AS avgIf, " +
        "         count(CASE WHEN p.international = true THEN 1 END) AS intlPaperCount } " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) MATCH (r)-[:INVENT]->(pat:Patent) " +
        "  WITH DISTINCT pat " +
        "  RETURN count(pat) AS patentCount, " +
        "         count(CASE WHEN pat.patentType = '发明专利' THEN 1 END) AS inventionCount } " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) MATCH (r)-[:INVENT]->(pat:Patent) " +
        "  WHERE pat.transferred = true " +
        "  WITH DISTINCT pat " +
        "  RETURN count(pat) AS transferredCount, sum(coalesce(pat.transferAmount, 0.0)) AS transferSum } " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) MATCH (r)-[:`CHARGE`|PARTICIPATE]->(proj:Project) " +
        "  WITH DISTINCT proj " +
        "  RETURN count(proj) AS projectCount, " +
        "         count(CASE WHEN proj.projLevel = '国家级' THEN 1 END) AS nationalProjects } " +
        "CALL { WITH i MATCH (r:Researcher)-[:BELONG_TO]->(i) MATCH (r)-[:COOPERATE_WITH]-(c:Researcher) " +
        "  OPTIONAL MATCH (c)-[:BELONG_TO]->(ci:Institution) " +
        "  WITH i, r, count(DISTINCT c) AS deg, " +
        "       count(DISTINCT CASE WHEN ci <> i THEN c END) AS crossDeg " +
        "  RETURN avg(deg) AS avgCooperators, sum(deg) AS coopTotal, sum(crossDeg) AS crossTotal } " +
        "RETURN instId, " +
        "       coalesce(i.instName, i.name, '未知机构') AS instName, " +
        "       i.type AS instType, " +
        "       researcherCount, avgInfluence, " +
        "       paperCount, avgIf, intlPaperCount, " +
        "       patentCount, inventionCount, " +
        "       transferredCount, transferSum, " +
        "       projectCount, nationalProjects, " +
        "       avgCooperators, coopTotal, crossTotal";

    /**
     * 获取指定机构的竞争力雷达图数据（7 维度）
     */
    public Map<String, Object> getCompetitivenessRadar(Long instId) {
        List<Map<String, Object>> list = compareInstitutions(Collections.singletonList(instId));
        if (!list.isEmpty()) {
            return list.get(0);
        }
        // 机构不存在
        Institution inst = institutionRepo.findById(instId).orElse(null);
        if (inst == null) return Collections.emptyMap();
        String instName = inst.getInstName() != null ? inst.getInstName() :
                          (inst.getName() != null ? inst.getName() : "未知机构");
        return emptyRadar(instName, instId);
    }

    /**
     * 多机构竞争力对比 — 单次 Cypher 查询，UNWIND 批量处理
     */
    public List<Map<String, Object>> compareInstitutions(List<Long> instIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(COMPARE_CYPHER, Map.of("instIds", instIds));
            while (result.hasNext()) {
                results.add(buildRadarResult(result.next()));
            }
        }
        return results;
    }

    /**
     * 根据原始统计数据构建 7 维雷达图结果
     * 维度得分口径（均为 0-100）：
     * 1. 科研规模 = (人员数 + 论文数 + 专利数 + 项目数) ÷ 10
     * 2. 科研质量 = 平均影响因子 × 10 + 国家级项目数 × 5
     * 3. 学术影响力 = 平均影响力指数 × 10
     * 4. 科研效率 = (论文数 + 专利数) ÷ 人员数 × 5
     * 5. 学术合作 = min(人均合作者数 × 5, 60) + min(跨机构合作占比% × 0.8, 40)
     * 6. 国际合作 = 国际合作论文占比% × 2.5
     * 7. 成果转化 = 专利转化率% × 0.5 + 已转化专利数 × 5
     */
    private Map<String, Object> buildRadarResult(Record rec) {
        long instId = rec.get("instId").asLong();
        String instName = rec.get("instName").asString();
        int researcherCount = rec.get("researcherCount").asInt();
        long paperCount = rec.get("paperCount").asLong();
        long patentCount = rec.get("patentCount").asLong();
        long projectCount = rec.get("projectCount").asLong();
        double avgIf = num(rec, "avgIf");
        long nationalProjects = rec.get("nationalProjects").asLong();
        double avgInfluence = num(rec, "avgInfluence");
        long inventionCount = rec.get("inventionCount").asLong();
        long intlPaperCount = rec.get("intlPaperCount").asLong();
        long transferredCount = rec.get("transferredCount").asLong();
        double transferSum = num(rec, "transferSum");
        double avgCooperators = num(rec, "avgCooperators");
        long coopTotal = rec.get("coopTotal").asLong();
        long crossTotal = rec.get("crossTotal").asLong();

        // 1. 科研规模
        double scale = Math.min((researcherCount + paperCount + patentCount + projectCount) / 10.0, 100);
        // 2. 科研质量
        double quality = Math.min((avgIf * 10) + (nationalProjects * 5), 100);
        // 3. 学术影响力
        double influence = Math.min(avgInfluence * 10, 100);
        // 4. 科研效率
        double efficiency = researcherCount > 0
            ? Math.min((paperCount + patentCount) * 5.0 / researcherCount, 100) : 0;
        // 5. 学术合作：人均合作者 + 跨机构合作占比
        double crossRatio = coopTotal > 0 ? crossTotal * 100.0 / coopTotal : 0;
        double cooperation = Math.min(avgCooperators * 5, 60) + Math.min(crossRatio * 0.8, 40);
        cooperation = Math.min(cooperation, 100);
        // 6. 国际合作：国际合作论文占比
        double intlRatio = paperCount > 0 ? intlPaperCount * 100.0 / paperCount : 0;
        double international = Math.min(intlRatio * 2.5, 100);
        // 7. 成果转化：转化率 + 已转化规模
        double transferRatio = patentCount > 0 ? transferredCount * 100.0 / patentCount : 0;
        double conversion = Math.min(transferRatio * 0.5 + transferredCount * 5.0, 100);

        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put("科研规模", round1(scale));
        dims.put("科研质量", round1(quality));
        dims.put("学术影响力", round1(influence));
        dims.put("科研效率", round1(efficiency));
        dims.put("学术合作", round1(cooperation));
        dims.put("国际合作", round1(international));
        dims.put("成果转化", round1(conversion));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("researcherCount", researcherCount);
        stats.put("paperCount", paperCount);
        stats.put("patentCount", patentCount);
        stats.put("projectCount", projectCount);
        stats.put("avgIf", round2(avgIf));
        stats.put("nationalProjects", nationalProjects);
        stats.put("avgInfluence", round2(avgInfluence));
        stats.put("inventionCount", inventionCount);
        stats.put("intlPaperCount", intlPaperCount);
        stats.put("transferredCount", transferredCount);
        stats.put("transferSum", round2(transferSum));
        stats.put("avgCooperators", round2(avgCooperators));
        stats.put("coopTotal", coopTotal);
        stats.put("crossTotal", crossTotal);
        stats.put("crossRatio", round2(crossRatio));
        stats.put("intlRatio", round2(intlRatio));
        stats.put("transferRatio", round2(transferRatio));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("institutionId", instId);
        result.put("institutionName", instName);
        result.put("researcherCount", researcherCount);
        result.put("dimensions", dims);
        result.put("stats", stats);
        return result;
    }

    /** 空机构雷达结果（无科研人员时） */
    private Map<String, Object> emptyRadar(String instName, Long instId) {
        Map<String, Double> dims = new LinkedHashMap<>();
        for (String d : DIMENSIONS) {
            dims.put(d, 0.0);
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        for (String k : new String[]{"researcherCount", "paperCount", "patentCount", "projectCount",
                "nationalProjects", "inventionCount", "intlPaperCount", "transferredCount",
                "coopTotal", "crossTotal"}) {
            stats.put(k, 0);
        }
        for (String k : new String[]{"avgIf", "avgInfluence", "transferSum", "avgCooperators",
                "crossRatio", "intlRatio", "transferRatio"}) {
            stats.put(k, 0.0);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("institutionId", instId);
        result.put("institutionName", instName);
        result.put("researcherCount", 0);
        result.put("dimensions", dims);
        result.put("stats", stats);
        return result;
    }

    /**
     * 雷达维度钻取：下钻查看构成该维度的二级指标、计算方式及其对应的原始成果清单
     */
    public Map<String, Object> drillDown(Long instId, String dimension) {
        Map<String, Object> radar = getCompetitivenessRadar(instId);
        if (radar.isEmpty()) return Collections.emptyMap();

        @SuppressWarnings("unchecked")
        Map<String, Double> dims = (Map<String, Double>) radar.get("dimensions");
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) radar.get("stats");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("institutionName", radar.get("institutionName"));
        result.put("dimension", dimension);
        result.put("score", dims.getOrDefault(dimension, 0.0));

        List<Map<String, Object>> level2 = new ArrayList<>();
        String formula;
        Map<String, Object> items;

        switch (dimension) {
            case "科研规模": {
                formula = "科研规模 = (科研人员数 + 论文数 + 专利数 + 项目数) ÷ 10，上限 100";
                addItem(level2, "科研人员数", stats.get("researcherCount"), "人");
                addItem(level2, "论文总数", stats.get("paperCount"), "篇");
                addItem(level2, "专利总数", stats.get("patentCount"), "项");
                addItem(level2, "项目总数", stats.get("projectCount"), "个");
                items = queryItems("机构科研人员清单（最多 20 条）",
                    new String[]{"姓名", "职称", "研究方向", "影响力指数"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "RETURN r.name AS c0, r.title AS c1, r.researchField AS c2, r.influenceIndex AS c3 " +
                    "ORDER BY c3 DESC LIMIT 20", instId);
                break;
            }
            case "科研质量": {
                formula = "科研质量 = min(平均影响因子 × 10 + 国家级项目数 × 5, 100)";
                addItem(level2, "平均影响因子", stats.get("avgIf"), "");
                addItem(level2, "国家级项目数", stats.get("nationalProjects"), "项");
                items = queryItems("机构论文清单（按影响因子降序，最多 20 条）",
                    new String[]{"论文标题", "期刊", "发表日期", "影响因子", "被引次数"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "MATCH (r)-[:WRITE]->(p:Paper) " +
                    "RETURN DISTINCT p.title AS c0, p.journal AS c1, toString(p.pubDate) AS c2, " +
                    "       p.impactFactor AS c3, p.citedNum AS c4 " +
                    "ORDER BY c3 DESC LIMIT 20", instId);
                break;
            }
            case "学术影响力": {
                formula = "学术影响力 = 机构科研人员平均影响力指数 × 10，上限 100";
                addItem(level2, "平均影响力指数", stats.get("avgInfluence"), "");
                addItem(level2, "科研人员数", stats.get("researcherCount"), "人");
                items = queryItems("机构科研人员清单（按影响力降序，最多 20 条）",
                    new String[]{"姓名", "职称", "学科门类", "影响力指数"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "RETURN r.name AS c0, r.title AS c1, r.disciplineCategory AS c2, r.influenceIndex AS c3 " +
                    "ORDER BY c3 DESC LIMIT 20", instId);
                break;
            }
            case "科研效率": {
                formula = "科研效率 = (论文数 + 专利数) ÷ 科研人员数 × 5，上限 100";
                long rc = ((Number) stats.get("researcherCount")).longValue();
                addItem(level2, "人均论文数", rc > 0 ? round2(((Number) stats.get("paperCount")).doubleValue() / rc) : 0, "篇/人");
                addItem(level2, "人均专利数", rc > 0 ? round2(((Number) stats.get("patentCount")).doubleValue() / rc) : 0, "项/人");
                addItem(level2, "人均项目数", rc > 0 ? round2(((Number) stats.get("projectCount")).doubleValue() / rc) : 0, "个/人");
                items = queryItems("机构科研人员产出明细（最多 20 条）",
                    new String[]{"姓名", "论文数", "专利数", "项目数"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "OPTIONAL MATCH (r)-[:WRITE]->(p:Paper) " +
                    "OPTIONAL MATCH (r)-[:INVENT]->(pat:Patent) " +
                    "OPTIONAL MATCH (r)-[:`CHARGE`|PARTICIPATE]->(proj:Project) " +
                    "WITH r, count(DISTINCT p) AS pc, count(DISTINCT pat) AS ptc, count(DISTINCT proj) AS prc " +
                    "RETURN r.name AS c0, pc AS c1, ptc AS c2, prc AS c3 " +
                    "ORDER BY pc + ptc + prc DESC LIMIT 20", instId);
                break;
            }
            case "学术合作": {
                formula = "学术合作 = min(人均合作者数 × 5, 60) + min(跨机构合作占比% × 0.8, 40)";
                addItem(level2, "人均合作者数", stats.get("avgCooperators"), "位/人");
                addItem(level2, "跨机构合作占比", stats.get("crossRatio"), "%");
                addItem(level2, "合作者人次（全部人员合作度数之和）", stats.get("coopTotal"), "人次");
                items = queryItems("机构科研人员合作明细（按合作者数降序，最多 20 条）",
                    new String[]{"姓名", "合作者数", "其中跨机构合作者数"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "OPTIONAL MATCH (r)-[:COOPERATE_WITH]-(c:Researcher) " +
                    "OPTIONAL MATCH (c)-[:BELONG_TO]->(ci:Institution) " +
                    "WITH i, r, count(DISTINCT c) AS deg, count(DISTINCT CASE WHEN ci <> i THEN c END) AS crossDeg " +
                    "RETURN r.name AS c0, deg AS c1, crossDeg AS c2 " +
                    "ORDER BY deg DESC LIMIT 20", instId);
                break;
            }
            case "国际合作": {
                formula = "国际合作 = min(国际合作论文占比% × 2.5, 100)，占比 = 国际合作论文数 ÷ 机构论文总数";
                addItem(level2, "国际合作论文数", stats.get("intlPaperCount"), "篇");
                addItem(level2, "论文总数", stats.get("paperCount"), "篇");
                addItem(level2, "国际合作论文占比", stats.get("intlRatio"), "%");
                items = queryItems("国际合作论文清单（最多 20 条）",
                    new String[]{"论文标题", "期刊", "发表日期", "被引次数", "影响因子"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "MATCH (r)-[:WRITE]->(p:Paper) WHERE p.international = true " +
                    "RETURN DISTINCT p.title AS c0, p.journal AS c1, toString(p.pubDate) AS c2, " +
                    "       p.citedNum AS c3, p.impactFactor AS c4 " +
                    "ORDER BY c3 DESC LIMIT 20", instId);
                break;
            }
            case "成果转化": {
                formula = "成果转化 = min(专利转化率% × 0.5 + 已转化专利数 × 5, 100)，转化率 = 已转化专利数 ÷ 专利总数";
                addItem(level2, "已转化专利数", stats.get("transferredCount"), "项");
                addItem(level2, "专利总数", stats.get("patentCount"), "项");
                addItem(level2, "专利转化率", stats.get("transferRatio"), "%");
                addItem(level2, "转化金额合计", stats.get("transferSum"), "万元");
                items = queryItems("已转化专利清单（按转化金额降序，最多 20 条）",
                    new String[]{"专利名称", "专利号", "转化金额(万元)", "授权日期"},
                    "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: $instId}) " +
                    "MATCH (r)-[:INVENT]->(pat:Patent) WHERE pat.transferred = true " +
                    "RETURN DISTINCT pat.name AS c0, pat.patentNo AS c1, pat.transferAmount AS c2, " +
                    "       toString(pat.grantDate) AS c3 " +
                    "ORDER BY c2 DESC LIMIT 20", instId);
                break;
            }
            default: {
                formula = "";
                items = queryItems("", new String[]{}, "RETURN 1 LIMIT 0", instId);
                break;
            }
        }

        result.put("formula", formula);
        result.put("level2", level2);
        result.put("items", items);
        return result;
    }

    /**
     * 生成机构竞争力对标分析简报（Markdown，可渲染为 PDF）
     * 传入列表的第一个机构视为"主机构（本校）"，其余为对标机构
     */
    public String generateBrief(List<Long> instIds) {
        if (instIds == null || instIds.isEmpty()) {
            return "# 错误\n\n未选择对标机构";
        }
        List<Map<String, Object>> radars = compareInstitutions(instIds);
        if (radars.isEmpty()) {
            return "# 错误\n\n未查询到对标数据";
        }
        // 按传入顺序排列（Cypher 返回顺序可能不同）
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> r : radars) {
            byId.put(((Number) r.get("institutionId")).longValue(), r);
        }
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Long id : instIds) {
            Map<String, Object> r = byId.get(id);
            if (r != null) ordered.add(r);
        }
        if (ordered.isEmpty()) ordered = radars;

        Map<String, Object> main = ordered.get(0);
        String mainName = (String) main.get("institutionName");
        List<String> names = ordered.stream()
            .map(r -> (String) r.get("institutionName"))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("# 机构竞争力对标分析简报\n\n");
        sb.append("**生成时间**: ").append(LocalDate.now()).append("\n\n");
        sb.append("**主机构（本校）**: ").append(mainName).append("\n\n");
        sb.append("**对标机构**: ").append(String.join("、", names)).append("\n\n");
        sb.append("---\n\n");

        // 一、综合评估概览
        sb.append("## 一、综合评估概览\n\n");
        sb.append("| 机构 | ").append(String.join(" | ", DIMENSIONS)).append(" | 平均分 |\n");
        StringBuilder headerSep = new StringBuilder("|:---");
        for (int i = 0; i <= DIMENSIONS.length; i++) headerSep.append("|:---");
        sb.append(headerSep).append("|\n");
        for (Map<String, Object> r : ordered) {
            Map<String, Double> dims = dimsOf(r);
            double sum = 0;
            for (String d : DIMENSIONS) sum += dims.getOrDefault(d, 0.0);
            double avgScore = sum / DIMENSIONS.length;
            StringBuilder row = new StringBuilder("| ").append(r.get("institutionName"));
            for (String d : DIMENSIONS) {
                row.append(" | ").append(String.format("%.1f", dims.getOrDefault(d, 0.0)));
            }
            row.append(" | ").append(String.format("%.1f", avgScore)).append(" |\n");
            sb.append(row);
        }
        sb.append("\n");

        // 二、相对优势领域 / 三、明显短板领域
        Map<String, Double> mainDims = dimsOf(main);
        @SuppressWarnings("unchecked")
        Map<String, Object> mainStats = (Map<String, Object>) main.get("stats");
        List<String> advantages = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> severeWeaknesses = new ArrayList<>();

        for (String d : DIMENSIONS) {
            double my = mainDims.getOrDefault(d, 0.0);
            double othersSum = 0;
            int n = 0;
            int rank = 1;
            for (int i = 1; i < ordered.size(); i++) {
                double other = dimsOf(ordered.get(i)).getOrDefault(d, 0.0);
                othersSum += other;
                n++;
                if (other > my) rank++;
            }
            double othersAvg = n > 0 ? othersSum / n : 0;
            double diff = my - othersAvg;
            if (diff > 0) {
                advantages.add(String.format("- **%s**：得分 %.1f，对标组平均 %.1f，领先 %.1f 分，位列第 %d。%s",
                    d, my, othersAvg, diff, rank, dimensionEvidence(d, mainStats)));
            } else if (diff < 0) {
                String line = String.format("- **%s**：得分 %.1f，对标组平均 %.1f，落后 %.1f 分，位列第 %d。%s",
                    d, my, othersAvg, -diff, rank, dimensionEvidence(d, mainStats));
                weaknesses.add(line);
                if (rank == ordered.size() && ordered.size() > 1) {
                    severeWeaknesses.add(line);
                }
            }
        }

        sb.append("## 二、相对优势领域\n\n");
        if (advantages.isEmpty()) {
            sb.append("- 本次对标中未见明显领先领域，整体与对标组持平或落后。\n");
        } else {
            for (String a : advantages) sb.append(a).append("\n");
        }
        sb.append("\n");

        sb.append("## 三、明显短板领域\n\n");
        if (weaknesses.isEmpty()) {
            sb.append("- 本次对标中未见落后领域，核心竞争力全面领先对标组。\n");
        } else {
            List<String> target = severeWeaknesses.isEmpty() ? weaknesses : severeWeaknesses;
            if (!severeWeaknesses.isEmpty()) {
                sb.append("> 以下为排名末位且落后对标组均值的关键短板：\n\n");
            }
            for (String w : target) sb.append(w).append("\n");
            if (!severeWeaknesses.isEmpty() && weaknesses.size() > severeWeaknesses.size()) {
                sb.append("\n其余相对不足项：");
                List<String> mild = weaknesses.stream().filter(w -> !severeWeaknesses.contains(w))
                        .map(w -> w.substring(2, Math.max(2, w.indexOf("："))))
                        .collect(Collectors.toList());
                sb.append(String.join("、", mild)).append("。\n");
            }
        }
        sb.append("\n");

        // 四、结论与建议
        sb.append("## 四、结论与建议\n\n");
        List<String> suggestions = new ArrayList<>();
        for (String d : DIMENSIONS) {
            double my = mainDims.getOrDefault(d, 0.0);
            double othersSum = 0;
            int n = 0;
            for (int i = 1; i < ordered.size(); i++) {
                othersSum += dimsOf(ordered.get(i)).getOrDefault(d, 0.0);
                n++;
            }
            if (n > 0 && my < othersSum / n) {
                suggestions.add("- " + dimensionSuggestion(d));
            }
        }
        if (suggestions.isEmpty()) {
            sb.append("- 主机构各维度均不低于对标组平均水平，建议保持当前发展策略，持续巩固领先优势，并关注新兴交叉领域的布局。\n");
        } else {
            sb.append("针对上述短板领域，建议：\n\n");
            for (String s : suggestions) sb.append(s).append("\n");
        }
        sb.append("\n---\n\n");
        sb.append("*本简报由科研人员画像分析系统自动生成，数据来源为图谱实时计算结果，供资源配置与政策调整参考。*\n");
        return sb.toString();
    }

    /** 各维度的关键数据支撑描述 */
    @SuppressWarnings("unchecked")
    private String dimensionEvidence(String dimension, Map<String, Object> stats) {
        if (stats == null) return "";
        switch (dimension) {
            case "科研规模":
                return String.format("数据支撑：科研人员 %s 人、论文 %s 篇、专利 %s 项、项目 %s 个。",
                    stats.get("researcherCount"), stats.get("paperCount"), stats.get("patentCount"), stats.get("projectCount"));
            case "科研质量":
                return String.format("数据支撑：平均影响因子 %s、国家级项目 %s 项。",
                    stats.get("avgIf"), stats.get("nationalProjects"));
            case "学术影响力":
                return String.format("数据支撑：平均影响力指数 %s。", stats.get("avgInfluence"));
            case "科研效率": {
                long rc = ((Number) stats.getOrDefault("researcherCount", 0)).longValue();
                long pc = ((Number) stats.getOrDefault("paperCount", 0)).longValue();
                long ptc = ((Number) stats.getOrDefault("patentCount", 0)).longValue();
                return String.format("数据支撑：人均论文 %.2f 篇、人均专利 %.2f 项。",
                    rc > 0 ? pc * 1.0 / rc : 0, rc > 0 ? ptc * 1.0 / rc : 0);
            }
            case "学术合作":
                return String.format("数据支撑：人均合作者 %s 位、跨机构合作占比 %s%%、合作者人次 %s。",
                    stats.get("avgCooperators"), stats.get("crossRatio"), stats.get("coopTotal"));
            case "国际合作":
                return String.format("数据支撑：国际合作论文 %s 篇、论文总数 %s 篇、占比 %s%%。",
                    stats.get("intlPaperCount"), stats.get("paperCount"), stats.get("intlRatio"));
            case "成果转化":
                return String.format("数据支撑：已转化专利 %s 项、专利总数 %s 项、转化率 %s%%、转化金额 %s 万元。",
                    stats.get("transferredCount"), stats.get("patentCount"), stats.get("transferRatio"), stats.get("transferSum"));
            default:
                return "";
        }
    }

    /** 各短板的针对性建议 */
    private String dimensionSuggestion(String dimension) {
        switch (dimension) {
            case "科研规模":
                return "**科研规模**：建议加大高层次人才引进力度，扩大科研队伍规模，稳步提升成果总量。";
            case "科研质量":
                return "**科研质量**：建议瞄准高影响因子期刊与国家级项目持续攻关，提升标志性成果质量。";
            case "学术影响力":
                return "**学术影响力**：建议加强代表性成果的推广与学术交流，提升团队学术声誉。";
            case "科研效率":
                return "**科研效率**：建议优化科研组织模式，提升人均产出效率，激发队伍活力。";
            case "学术合作":
                return "**学术合作**：建议拓展跨机构、跨学科合作网络，主动参与重大联合攻关项目。";
            case "国际合作":
                return "**国际合作**：建议加强国际交流，推动与国际一流团队的联合研究与联合发表。";
            case "成果转化":
                return "**成果转化**：建议完善成果转化激励机制，推动专利技术与产业需求精准对接。";
            default:
                return dimension + "：建议结合自身定位制定针对性提升方案。";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> dimsOf(Map<String, Object> radar) {
        Object dims = radar.get("dimensions");
        return dims instanceof Map ? (Map<String, Double>) dims : Collections.emptyMap();
    }

    /**
     * 查询所有机构列表（二级树结构）
     * 返回格式: [
     *   { id, name, type, level:1, children: [{id, name, type, level:2, parentId}, ...] },
     *   ...
     * ]
     */
    public List<Map<String, Object>> findAllSimple() {
        List<Institution> all = institutionRepo.findAll();

        // 分组：无 parentId 的为一级，有 parentId 的为二级
        Map<Long, Map<String, Object>> parentMap = new LinkedHashMap<>();
        List<Map<String, Object>> orphanChildren = new ArrayList<>();

        for (Institution inst : all) {
            String displayName = inst.getInstName() != null ? inst.getInstName() :
                                 (inst.getName() != null ? inst.getName() : "未知机构");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", inst.getId());
            item.put("name", displayName);
            item.put("type", inst.getType() != null ? inst.getType() : "");
            item.put("parentId", inst.getParentId());

            if (inst.getParentId() == null) {
                item.put("level", 1);
                item.put("children", new ArrayList<>());
                parentMap.put(inst.getId(), item);
            } else {
                item.put("level", 2);
                orphanChildren.add(item);
            }
        }

        // 将子节点挂到父节点上
        for (Map<String, Object> child : orphanChildren) {
            Long pid = (Long) child.get("parentId");
            Map<String, Object> parent = parentMap.get(pid);
            if (parent != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(child);
            } else {
                // 父节点不存在，作为一级处理
                child.put("level", 1);
                child.put("children", new ArrayList<>());
                parentMap.put((Long) child.get("id"), child);
            }
        }

        return new ArrayList<>(parentMap.values());
    }

    // ============================
    // 工具方法
    // ============================

    /** 安全读取数值字段（null 返回 0） */
    private double num(Record rec, String field) {
        return rec.get(field).isNull() ? 0.0 : rec.get(field).asDouble();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void addItem(List<Map<String, Object>> list, String name, Object value, String unit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("unit", unit);
        list.add(m);
    }

    /** 执行原始成果清单查询（c0..cN 列别名） */
    private Map<String, Object> queryItems(String title, String[] columns, String cypher, Long instId) {
        List<List<Object>> rows = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("instId", instId));
            while (result.hasNext()) {
                Record rec = result.next();
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < columns.length; i++) {
                    row.add(valueToObj(rec.get("c" + i)));
                }
                rows.add(row);
            }
        }
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("title", title);
        items.put("columns", Arrays.asList(columns));
        items.put("rows", rows);
        return items;
    }

    /** Neo4j Value 转 Java 对象 */
    private Object valueToObj(org.neo4j.driver.Value v) {
        if (v == null || v.isNull()) return null;
        switch (v.type().name()) {
            case "INTEGER":
                return v.asLong();
            case "FLOAT":
                return v.asDouble();
            case "BOOLEAN":
                return v.asBoolean();
            default:
                return v.asString();
        }
    }
}
