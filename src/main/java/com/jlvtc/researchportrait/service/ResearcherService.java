package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.*;
import com.jlvtc.researchportrait.repository.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Period;

import lombok.extern.slf4j.Slf4j;

/**
 * 科研人员 业务层
 */
@Slf4j
@Service
public class ResearcherService {

    @Autowired
    private ResearcherRepository researcherRepository;
    
    @Autowired
    private PaperRepository paperRepository;
    
    @Autowired
    private PatentRepository patentRepository;
    
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InfluenceCalculatorService influenceCalculatorService;

    @Autowired
    private Driver driver;

    /**
     * 保存/更新科研人员
     */
    public Researcher saveResearcher(Researcher researcher) {
        return researcherRepository.save(researcher);
    }

    /**
     * 根据ID查询
     */
    public Researcher findById(Long id) {
        Optional<Researcher> optional = researcherRepository.findById(id);
        return optional.orElse(null);
    }

    /**
     * 查询所有科研人员
     */
    public List<Researcher> findAll() {
        return researcherRepository.findAll();
    }

    /**
     * 删除
     */
    public void deleteById(Long id) {
        researcherRepository.deleteById(id);
    }

    /**
     * 获取科研人员多维画像详情
     */
    public Map<String, Object> getProfileDetail(Long id) {
        Researcher r = findById(id);
        if (r == null) return Collections.emptyMap();

        Map<String, Object> detail = new HashMap<>();
        detail.put("basicInfo", r);
        
        // 1. 产出趋势统计 (按年份)
        Map<Integer, Long> paperTrend = r.getPapers().stream()
            .filter(p -> p.getPubDate() != null)
            .collect(Collectors.groupingBy(p -> p.getPubDate().getYear(), Collectors.counting()));
        
        Map<Integer, Long> patentTrend = r.getPatents().stream()
            .filter(p -> p.getApplyDate() != null)
            .collect(Collectors.groupingBy(p -> p.getApplyDate().getYear(), Collectors.counting()));
            
        detail.put("trends", Map.of("papers", paperTrend, "patents", patentTrend));
        
        // 2. 项目经费统计
        double totalFund = r.getChargeProjects().stream()
            .mapToDouble(p -> p.getFund() != null ? p.getFund() : 0)
            .sum();
        detail.put("totalFunding", totalFund);
        
        // 3. 研究主题演变 (提取论文标题关键词)
        Map<Integer, List<String>> topicEvolution = r.getPapers().stream()
            .filter(p -> p.getPubDate() != null && p.getTitle() != null)
            .collect(Collectors.groupingBy(
                p -> p.getPubDate().getYear(),
                Collectors.mapping(p -> extractKeywords(p.getTitle()), Collectors.toList())
            ));
        detail.put("topicEvolution", topicEvolution);
        
        // 4. 智能标签体系（多源成果数据融合自动生成）
        detail.put("intelligentTags", buildIntelligentTags(r));
        
        // 5. 项目层级分布（负责 + 参与的所有项目按层级分组）
        Map<String, Long> levelDist = new LinkedHashMap<>();
        for (String level : new String[]{"国家级", "省部级", "横向"}) {
            long cnt = r.getChargeProjects().stream()
                    .filter(p -> level.equals(p.getProjLevel())).count()
                    + r.getJoinProjects().stream()
                    .filter(p -> level.equals(p.getProjLevel())).count();
            levelDist.put(level, cnt);
        }
        detail.put("projectLevelDistribution", levelDist);
        
        return detail;
    }

    /**
     * 构建智能标签体系：基于项目、论文、专利等多源数据自动推理
     * 覆盖研究方向、学术影响力、科研产出、项目层级、合作交流五个维度
     */
    public List<Map<String, Object>> buildIntelligentTags(Researcher r) {
        List<Map<String, Object>> tags = new ArrayList<>();
        List<Paper> papers = r.getPapers();
        List<Patent> patents = r.getPatents();

        // 1. 研究方向维度
        if (r.getDisciplineCategory() != null) {
            tags.add(tag(r.getDisciplineCategory() + "学者", "研究方向"));
        }
        if (r.getResearchField() != null) {
            tags.add(tag("核心方向：" + r.getResearchField(), "研究方向"));
        }

        // 2. 学术影响力维度
        double influence = r.getInfluenceIndex() != null ? r.getInfluenceIndex() : 0;
        if (influence >= 85) {
            tags.add(tag("高影响力学者", "学术影响力"));
        } else if (influence >= 65) {
            tags.add(tag("中坚学者", "学术影响力"));
        } else if (influence >= 45) {
            tags.add(tag("潜力学者", "学术影响力"));
        }

        // 3. 科研产出维度（论文被引 / 发明专利 / 成果转化 / 国际合作）
        int totalCited = papers.stream()
                .filter(p -> p.getCitedNum() != null).mapToInt(Paper::getCitedNum).sum();
        if (totalCited >= 300) {
            tags.add(tag("高被引学者（累计被引 " + totalCited + " 次）", "科研产出"));
        }
        long inventionCount = patents.stream()
                .filter(p -> "发明专利".equals(p.getPatentType())).count();
        if (inventionCount >= 5) {
            tags.add(tag("发明专利达人（" + inventionCount + " 项）", "科研产出"));
        }
        long transferredCount = patents.stream()
                .filter(p -> Boolean.TRUE.equals(p.getTransferred())).count();
        if (transferredCount >= 1) {
            tags.add(tag("成果转化先锋（" + transferredCount + " 项）", "科研产出"));
        }
        long intlCount = papers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getInternational())).count();
        if (intlCount >= 1) {
            tags.add(tag("国际合著（" + intlCount + " 篇）", "科研产出"));
        }

        // 4. 项目层级维度
        long nationalCharge = r.getChargeProjects().stream()
                .filter(p -> "国家级".equals(p.getProjLevel())).count();
        if (nationalCharge >= 1) {
            tags.add(tag("国家级项目负责人（" + nationalCharge + " 项）", "项目层级"));
        }
        if (r.getChargeProjects().size() >= 3) {
            tags.add(tag("项目攻关骨干", "项目层级"));
        }

        // 5. 合作交流维度（基于合作网络中推导的合作者规模）
        long cooperatorCount = countCooperators(r.getId());
        if (cooperatorCount >= 8) {
            tags.add(tag("合作网络活跃（" + cooperatorCount + " 位合作者）", "合作交流"));
        }

        return tags;
    }

    /** 构造单个标签项 */
    private Map<String, Object> tag(String name, String category) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("category", category);
        return t;
    }

    /** 统计合作者数量（COOPERATE_WITH 无向关系） */
    private long countCooperators(Long researcherId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (r:Researcher {id: $rid})-[:COOPERATE_WITH]-(c:Researcher) RETURN count(c) AS cnt",
                Map.of("rid", researcherId));
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
        } catch (Exception e) {
            log.warn("统计合作者数量失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 简单的关键词提取模拟（实际生产应接入 HanLP 或 Jieba）
     */
    private String extractKeywords(String title) {
        // 这里简单按空格或标点分割，并过滤掉常见停用词
        String[] words = title.split("[\\s,，。、]" );
        for (String word : words) {
            if (word.length() > 1) return word; // 返回第一个有意义的词作为示例
        }
        return "综合研究";
    }

    /**
     * 高级人才筛选 — 优化版：Cypher 查询直接在 Neo4j 中过滤
     * 避免 findAll() 加载全部研究者及关联关系导致超时
     */
    public List<Researcher> advancedSearch(Map<String, Object> criteria) {
        String title = getStr(criteria, "title");
        String discipline = getStr(criteria, "discipline");
        String minInfluence = getStr(criteria, "minInfluence");
        boolean nationalOnly = "国家级".equals(getStr(criteria, "projLevel"));

        StringBuilder cypher = new StringBuilder("MATCH (r:Researcher) WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // 职称筛选（精确匹配）
        if (title != null && !title.isEmpty()) {
            cypher.append(" AND r.title = $title");
            params.put("title", title);
        }
        // 学科门类筛选（包含匹配）
        if (discipline != null && !discipline.isEmpty()) {
            cypher.append(" AND r.disciplineCategory CONTAINS $discipline");
            params.put("discipline", discipline);
        }
        // 影响力指数下限筛选
        if (minInfluence != null && !minInfluence.isEmpty()) {
            try {
                double min = Double.parseDouble(minInfluence);
                cypher.append(" AND r.influenceIndex >= $minInfluence");
                params.put("minInfluence", min);
            } catch (NumberFormatException e) {
                // 忽略无效数字输入
            }
        }
        // 国家级项目负责人筛选
        if (nationalOnly) {
            cypher.append(" AND EXISTS { MATCH (r)-[:`CHARGE`]->(p:Project) WHERE p.projLevel = '国家级' }");
        }

        cypher.append(" RETURN r ORDER BY r.influenceIndex DESC");

        List<Researcher> results = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher.toString(), params);
            while (result.hasNext()) {
                Record record = result.next();
                org.neo4j.driver.types.Node node = record.get("r").asNode();
                Researcher r = new Researcher();
                r.setId(node.get("id").asLong());
                r.setName(node.get("name").isNull() ? null : node.get("name").asString());
                r.setAge(node.get("age").isNull() ? null : node.get("age").asInt());
                r.setDepartment(node.get("department").isNull() ? null : node.get("department").asString());
                r.setTitle(node.get("title").isNull() ? null : node.get("title").asString());
                r.setResearchField(node.get("researchField").isNull() ? null : node.get("researchField").asString());
                r.setDisciplineCategory(node.get("disciplineCategory").isNull() ? null : node.get("disciplineCategory").asString());
                r.setInfluenceIndex(node.get("influenceIndex").isNull() ? null : node.get("influenceIndex").asDouble());
                results.add(r);
            }
        }
        return results;
    }

    /**
     * 安全提取 Map 中的字符串值
     */
    private String getStr(Map<String, Object> criteria, String key) {
        Object val = criteria.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * 刷新单个科研人员的影响力指数
     */
    @Transactional("transactionManager")
    public Researcher refreshInfluenceIndex(Long id) {
        Researcher r = findById(id);
        if (r == null) return null;
        double newIndex = influenceCalculatorService.calculateInfluenceIndex(
            r, r.getPapers(), r.getPatents(), r.getChargeProjects()
        );
        r.setInfluenceIndex(newIndex);
        return researcherRepository.save(r);
    }

    /**
     * 导出所有科研人员基础信息（轻量级 Cypher 查询，不加载关联关系）
     * 仅查询导出 Excel 所需的 5 个字段，避免 findAll() 导致 OOM
     */
    public List<Map<String, Object>> exportAllSimple() {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (r:Researcher) RETURN r.name AS name, r.title AS title, " +
                "r.department AS department, r.researchField AS researchField, " +
                "r.influenceIndex AS influenceIndex ORDER BY r.influenceIndex DESC"
            );
            while (result.hasNext()) {
                Record rec = result.next();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("姓名", rec.get("name").isNull() ? null : rec.get("name").asString());
                map.put("职称", rec.get("title").isNull() ? null : rec.get("title").asString());
                map.put("所属院系", rec.get("department").isNull() ? null : rec.get("department").asString());
                map.put("研究方向", rec.get("researchField").isNull() ? null : rec.get("researchField").asString());
                map.put("影响力指数", rec.get("influenceIndex").isNull() ? null : rec.get("influenceIndex").asDouble());
                results.add(map);
            }
        }
        return results;
    }

    /**
     * 全量刷新所有科研人员的影响力指数（定时任务与手动触发共用）
     * 不使用 @Transactional，改用纯 Cypher 读写，避免事务缓存导致 OOM
     * @return 已刷新的人员数
     */
    public int refreshAllInfluenceIndex() {
        // 轻量级查询获取所有科研人员 Neo4j 内部 ID
        List<Long> allIds = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (r:Researcher) RETURN id(r) AS rid");
            while (result.hasNext()) {
                allIds.add(result.next().get("rid").asLong());
            }
        }
        log.info("[影响力刷新] 共查询到 {} 名科研人员，开始分批处理", allIds.size());
        return processInfluenceBatch(allIds);
    }

    /**
     * 按院系刷新影响力指数（仅处理所选院系的科研人员）
     * @param instIds 机构 ID 列表
     * @return 已刷新的人员数
     */
    public int refreshInfluenceByInstitutions(List<Long> instIds) {
        List<Long> researcherIds = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(
                "UNWIND $instIds AS instId " +
                "MATCH (r:Researcher)-[:BELONG_TO]->(i:Institution {id: instId}) " +
                "RETURN id(r) AS rid",
                Map.of("instIds", instIds)
            );
            while (result.hasNext()) {
                researcherIds.add(result.next().get("rid").asLong());
            }
        }
        log.info("[影响力刷新] 所选院系关联 {} 名科研人员，开始分批处理", researcherIds.size());
        return processInfluenceBatch(researcherIds);
    }

    /**
     * 分批处理影响力计算（纯 Cypher，不依赖 Spring Data 事务缓存）
     * 权重与衰减系数统一从 WeightConfig（动态权重配置）读取，管理员调整后即时生效
     */
    private int processInfluenceBatch(List<Long> allIds) {
        int batchSize = 50;
        int count = 0;
        // 缓存各学科权重配置，避免逐人重复查询数据库
        Map<String, WeightConfig> configCache = new HashMap<>();
        for (int i = 0; i < allIds.size(); i += batchSize) {
            List<Long> batchIds = allIds.subList(i, Math.min(i + batchSize, allIds.size()));
            for (Long rid : batchIds) {
                try (Session session = driver.session()) {
                    // 单独加载一个科研人员及其关联数据
                    Result rResult = session.run(
                        "MATCH (r) WHERE id(r) = $rid " +
                        "OPTIONAL MATCH (r)-[:WRITE]->(p:Paper) " +
                        "OPTIONAL MATCH (r)-[:INVENT]->(pat:Patent) " +
                        "OPTIONAL MATCH (r)-[:`CHARGE`]->(proj:Project) " +
                        "RETURN r, collect(DISTINCT p) AS papers, collect(DISTINCT pat) AS patents, collect(DISTINCT proj) AS projects",
                        Map.of("rid", rid)
                    );
                    if (!rResult.hasNext()) continue;
                    Record rec = rResult.next();
                    org.neo4j.driver.types.Node rNode = rec.get("r").asNode();

                    // 学科权重配置：数据库动态配置优先（未配置时使用默认模板）
                    double totalScore = 0.0;
                    String discipline = rNode.containsKey("disciplineCategory") && !rNode.get("disciplineCategory").isNull()
                        ? rNode.get("disciplineCategory").asString() : null;
                    WeightConfig config = resolveCachedConfig(discipline, configCache);
                    double lambda = config.getDecayRate() != null ? config.getDecayRate() : 0.05;

                    // 论文得分（按发表时间衰减）
                    List<org.neo4j.driver.types.Node> papers = rec.get("papers").asList(v -> v.asNode());
                    for (org.neo4j.driver.types.Node p : papers) {
                        double cited = p.containsKey("citedNum") && !p.get("citedNum").isNull() ? p.get("citedNum").asDouble() : 0;
                        double if_ = p.containsKey("impactFactor") && !p.get("impactFactor").isNull() ? p.get("impactFactor").asDouble() : 0;
                        double baseScore = cited * 0.5 + if_ * 2.0;
                        double decay = computeDecay(p, "pubDate", lambda);
                        totalScore += baseScore * decay * config.getPaperWeight();
                    }

                    // 专利得分（按授权时间衰减）
                    List<org.neo4j.driver.types.Node> patents = rec.get("patents").asList(v -> v.asNode());
                    for (org.neo4j.driver.types.Node pt : patents) {
                        String patentType = pt.containsKey("patentType") && !pt.get("patentType").isNull()
                            ? pt.get("patentType").asString() : "";
                        double baseScore = "发明专利".equals(patentType) ? 10.0 : 5.0;
                        double decay = computeDecay(pt, "grantDate", lambda);
                        totalScore += baseScore * decay * config.getPatentWeight();
                    }

                    // 项目得分（按结题时间衰减，无结题时间时回退到开始日期）
                    List<org.neo4j.driver.types.Node> projects = rec.get("projects").asList(v -> v.asNode());
                    for (org.neo4j.driver.types.Node pr : projects) {
                        double fund = pr.containsKey("fund") && !pr.get("fund").isNull() ? pr.get("fund").asDouble() : 0;
                        double baseScore = fund * 0.1;
                        String projLevel = pr.containsKey("projLevel") && !pr.get("projLevel").isNull()
                            ? pr.get("projLevel").asString() : "";
                        if ("国家级".equals(projLevel)) baseScore *= 1.5;
                        double decay = computeDecay(pr, "endDate", lambda, "startDate");
                        totalScore += baseScore * decay * config.getProjectWeight();
                    }

                    double newIndex = Math.round(totalScore * 100.0) / 100.0;
                    session.run("MATCH (r) WHERE id(r) = $rid SET r.influenceIndex = $idx",
                        Map.of("rid", rid, "idx", newIndex));
                    count++;
                }
            }
            log.info("[影响力刷新] 进度 {}/{}", Math.min(i + batchSize, allIds.size()), allIds.size());
        }
        return count;
    }

    /** 解析并对学科权重配置做缓存（批量刷新时避免重复查询） */
    private WeightConfig resolveCachedConfig(String discipline, Map<String, WeightConfig> cache) {
        String key = discipline == null ? "" : discipline;
        if (!cache.containsKey(key)) {
            cache.put(key, influenceCalculatorService.resolveWeightConfig(discipline));
        }
        return cache.get(key);
    }

    /** 时间衰减函数: W(t) = e^(-lambda * years)，未来时间按当前年处理 */
    private double computeDecay(org.neo4j.driver.types.Node node, String dateField, double lambda) {
        return computeDecay(node, dateField, lambda, null);
    }

    /** 时间衰减函数（带备用日期字段）：主字段缺失时回退到备用字段 */
    private double computeDecay(org.neo4j.driver.types.Node node, String dateField, double lambda, String fallbackField) {
        LocalDate date = readDate(node, dateField);
        if (date == null && fallbackField != null) date = readDate(node, fallbackField);
        if (date == null) return 1.0;
        int years = Period.between(date, LocalDate.now()).getYears();
        if (years < 0) years = 0;
        return Math.exp(-lambda * years);
    }

    /** 安全读取节点日期字段 */
    private LocalDate readDate(org.neo4j.driver.types.Node node, String field) {
        if (!node.containsKey(field) || node.get(field).isNull()) return null;
        try {
            return node.get(field).asLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成科研人员智能分析报告（Markdown 格式）
     */
    public String generateAnalysisReport(Long id) {
        Researcher r = findById(id);
        if (r == null) return "# 错误\n\n未找到科研人员 id=" + id;

        Map<String, Object> profile = getProfileDetail(id);
        StringBuilder sb = new StringBuilder();

        // 报告标题
        sb.append("# 科研人员智能分析报告\n\n");
        sb.append("**生成时间**: ").append(LocalDate.now()).append("\n\n");
        sb.append("---\n\n");

        // 一、基本信息
        sb.append("## 一、基本信息\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|:---|:---|\n");
        sb.append("| 姓名 | ").append(r.getName()).append(" |\n");
        sb.append("| 年龄 | ").append(r.getAge() != null ? r.getAge() : "-").append(" |\n");
        sb.append("| 所属院系 | ").append(r.getDepartment() != null ? r.getDepartment() : "-").append(" |\n");
        sb.append("| 职称 | ").append(r.getTitle() != null ? r.getTitle() : "-").append(" |\n");
        sb.append("| 研究方向 | ").append(r.getResearchField() != null ? r.getResearchField() : "-").append(" |\n");
        sb.append("| 学科门类 | ").append(r.getDisciplineCategory() != null ? r.getDisciplineCategory() : "-").append(" |\n");
        sb.append("| 隶属机构 | ").append(r.getInstitution() != null ? (r.getInstitution().getInstName() != null ? r.getInstitution().getInstName() : r.getInstitution().getName()) : "-").append(" |\n");
        sb.append("| 影响力指数 | ").append(r.getInfluenceIndex() != null ? String.format("%.2f", r.getInfluenceIndex()) : "待计算").append(" |\n\n");

        // 二、智能标签体系
        sb.append("## 二、智能标签体系\n\n");
        sb.append("> 基于科研项目、学术论文、授权专利等多源成果数据自动分析生成\n\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tags = (List<Map<String, Object>>) profile.get("intelligentTags");
        if (tags != null && !tags.isEmpty()) {
            sb.append("| 标签类别 | 标签内容 |\n");
            sb.append("|:---|:---|\n");
            Map<String, List<String>> groupedTags = new LinkedHashMap<>();
            for (Map<String, Object> t : tags) {
                String cat = String.valueOf(t.get("category"));
                groupedTags.computeIfAbsent(cat, k -> new ArrayList<>()).add(String.valueOf(t.get("name")));
            }
            for (Map.Entry<String, List<String>> e : groupedTags.entrySet()) {
                sb.append("| ").append(e.getKey()).append(" | ").append(String.join("；", e.getValue())).append(" |\n");
            }
        } else {
            sb.append("暂无足够数据生成智能标签\n");
        }
        sb.append("\n");

        // 三、科研产出统计
        sb.append("## 三、科研产出统计\n\n");
        int paperCount = r.getPapers().size();
        int patentCount = r.getPatents().size();
        int projectCount = r.getChargeProjects().size() + r.getJoinProjects().size();
        sb.append("- 论文总数: **").append(paperCount).append("** 篇\n");
        sb.append("- 专利总数: **").append(patentCount).append("** 项\n");
        sb.append("- 参与项目: **").append(projectCount).append("** 个（其中负责 ").append(r.getChargeProjects().size()).append(" 个）\n");
        sb.append("- 累计项目经费: **").append(String.format("%.2f", profile.get("totalFunding"))).append("** 万元\n\n");

        // 四、论文质量分析
        sb.append("## 四、论文质量分析\n\n");
        int idx; // 序号计数器，在各清单中复用
        if (paperCount > 0) {
            double avgIf = r.getPapers().stream()
                .filter(p -> p.getImpactFactor() != null)
                .mapToDouble(Paper::getImpactFactor)
                .average().orElse(0);
            int totalCited = r.getPapers().stream()
                .filter(p -> p.getCitedNum() != null)
                .mapToInt(Paper::getCitedNum)
                .sum();
            sb.append("- 平均影响因子: **").append(String.format("%.2f", avgIf)).append("**\n");
            sb.append("- 总被引次数: **").append(totalCited).append("** 次\n");
            sb.append("- 最高被引论文: ");
            r.getPapers().stream()
                .filter(p -> p.getCitedNum() != null)
                .max(Comparator.comparingInt(Paper::getCitedNum))
                .ifPresentOrElse(
                    p -> sb.append("《").append(p.getTitle()).append("》（被引 ").append(p.getCitedNum()).append(" 次）\n"),
                    () -> sb.append("-\n")
                );
            sb.append("\n### 论文清单\n\n");
            sb.append("| 序号 | 论文标题 | 期刊 | 发表年份 | 被引 | 影响因子 |\n");
            sb.append("|:---|:---|:---|:---|:---|:---|\n");
            idx = 1;
            for (Paper p : r.getPapers()) {
                sb.append("| ").append(idx++).append(" | ")
                  .append(p.getTitle() != null ? p.getTitle() : "-").append(" | ")
                  .append(p.getJournal() != null ? p.getJournal() : "-").append(" | ")
                  .append(p.getPubDate() != null ? p.getPubDate().getYear() : "-").append(" | ")
                  .append(p.getCitedNum() != null ? p.getCitedNum() : "-").append(" | ")
                  .append(p.getImpactFactor() != null ? String.format("%.2f", p.getImpactFactor()) : "-").append(" |\n");
            }
        } else {
            sb.append("暂无论文数据\n");
        }
        sb.append("\n");

        // 五、专利分析
        sb.append("## 五、专利分析\n\n");
        if (patentCount > 0) {
            long inventionCount = r.getPatents().stream()
                .filter(p -> "发明专利".equals(p.getPatentType())).count();
            long grantedCount = r.getPatents().stream()
                .filter(p -> "授权".equals(p.getStatus())).count();
            sb.append("- 发明专利: **").append(inventionCount).append("** 项\n");
            sb.append("- 已授权专利: **").append(grantedCount).append("** 项\n");
            sb.append("- 专利授权率: **").append(patentCount > 0 ? String.format("%.1f%%", grantedCount * 100.0 / patentCount) : "0%").append("**\n\n");
            sb.append("### 专利清单\n\n");
            sb.append("| 序号 | 专利名称 | 专利号 | 类型 | 状态 |\n");
            sb.append("|:---|:---|:---|:---|:---|\n");
            idx = 1;
            for (Patent p : r.getPatents()) {
                sb.append("| ").append(idx++).append(" | ")
                  .append(p.getName() != null ? p.getName() : "-").append(" | ")
                  .append(p.getPatentNo() != null ? p.getPatentNo() : "-").append(" | ")
                  .append(p.getPatentType() != null ? p.getPatentType() : "-").append(" | ")
                  .append(p.getStatus() != null ? p.getStatus() : "-").append(" |\n");
            }
        } else {
            sb.append("暂无专利数据\n");
        }
        sb.append("\n");

        // 六、项目分析
        sb.append("## 六、科研项目分析\n\n");
        if (!r.getChargeProjects().isEmpty() || !r.getJoinProjects().isEmpty()) {
            long nationalCount = r.getChargeProjects().stream()
                .filter(p -> "国家级".equals(p.getProjLevel())).count();
            sb.append("- 负责项目: **").append(r.getChargeProjects().size()).append("** 个\n");
            sb.append("- 参与项目: **").append(r.getJoinProjects().size()).append("** 个\n");
            sb.append("- 国家级项目: **").append(nationalCount).append("** 个\n\n");
            // 项目层级分布
            @SuppressWarnings("unchecked")
            Map<String, Long> levelDist = (Map<String, Long>) profile.get("projectLevelDistribution");
            if (levelDist != null && !levelDist.isEmpty()) {
                sb.append("**项目层级分布**：\n\n");
                sb.append("| 项目层级 | 数量 |\n");
                sb.append("|:---|:---|\n");
                for (Map.Entry<String, Long> e : levelDist.entrySet()) {
                    sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n");
                }
                sb.append("\n");
            }
            sb.append("### 项目清单\n\n");
            sb.append("| 序号 | 项目名称 | 级别 | 经费(万元) | 状态 | 角色 |\n");
            sb.append("|:---|:---|:---|:---|:---|:---|\n");
            idx = 1;
            for (Project p : r.getChargeProjects()) {
                sb.append("| ").append(idx++).append(" | ")
                  .append(p.getProjName() != null ? p.getProjName() : "-").append(" | ")
                  .append(p.getProjLevel() != null ? p.getProjLevel() : "-").append(" | ")
                  .append(p.getFund() != null ? String.format("%.2f", p.getFund()) : "-").append(" | ")
                  .append(p.getStatus() != null ? p.getStatus() : "-").append(" | 负责人 |\n");
            }
            for (Project p : r.getJoinProjects()) {
                sb.append("| ").append(idx++).append(" | ")
                  .append(p.getProjName() != null ? p.getProjName() : "-").append(" | ")
                  .append(p.getProjLevel() != null ? p.getProjLevel() : "-").append(" | ")
                  .append(p.getFund() != null ? String.format("%.2f", p.getFund()) : "-").append(" | ")
                  .append(p.getStatus() != null ? p.getStatus() : "-").append(" | 参与人 |\n");
            }
        } else {
            sb.append("暂无项目数据\n");
        }
        sb.append("\n");

        // 七、产出趋势分析
        sb.append("## 七、产出趋势分析\n\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> trends = (Map<String, Object>) profile.get("trends");
        if (trends != null) {
            @SuppressWarnings("unchecked")
            Map<Integer, Long> paperTrend = (Map<Integer, Long>) trends.get("papers");
            @SuppressWarnings("unchecked")
            Map<Integer, Long> patentTrend = (Map<Integer, Long>) trends.get("patents");
            sb.append("| 年份 | 论文数 | 专利数 |\n");
            sb.append("|:---|:---|:---|\n");
            Set<Integer> years = new TreeSet<>();
            if (paperTrend != null) years.addAll(paperTrend.keySet());
            if (patentTrend != null) years.addAll(patentTrend.keySet());
            for (Integer year : years) {
                sb.append("| ").append(year).append(" | ")
                  .append(paperTrend != null && paperTrend.containsKey(year) ? paperTrend.get(year) : 0).append(" | ")
                  .append(patentTrend != null && patentTrend.containsKey(year) ? patentTrend.get(year) : 0).append(" |\n");
            }
        }
        sb.append("\n");

        // 八、综合评价
        sb.append("## 八、综合评价\n\n");
        double influence = r.getInfluenceIndex() != null ? r.getInfluenceIndex() : 0;
        String level;
        if (influence >= 80) {
 level = "优秀，处于学术带头人水平";
        } else if (influence >= 60) {
 level = "良好，具有较强的科研能力";
        } else if (influence >= 40) {
 level = "中等，有一定科研产出";
        } else {
 level = "待提升，建议加强科研产出";
        }
        sb.append("- 学术影响力指数: **").append(String.format("%.2f", influence)).append("** —— ").append(level).append("\n");
        sb.append("- 科研活跃度: ");
        int recentCount = 0;
        int currentYear = LocalDate.now().getYear();
        if (paperTrendAvailable(profile)) {
            recentCount = countRecentPapers(r, currentYear);
        }
        sb.append(recentCount >= 3 ? "高（近3年有持续产出）" : recentCount >= 1 ? "中（近3年有产出）" : "低（近3年无产出）");
        sb.append("\n");
        sb.append("- 学科贡献: 涉及").append(r.getDisciplineCategory() != null ? r.getDisciplineCategory() : "未知").append("领域\n");
        sb.append("\n---\n\n");
        sb.append("*本报告由科研人员画像分析系统自动生成，仅供参考。*\n");

        return sb.toString();
    }

    /**
     * 判断产出趋势数据是否可用
     */
    private boolean paperTrendAvailable(Map<String, Object> profile) {
        Object trends = profile.get("trends");
        return trends instanceof Map && !((Map<?, ?>) trends).isEmpty();
    }

    /**
     * 统计近3年论文数量
     */
    private int countRecentPapers(Researcher r, int currentYear) {
        return (int) r.getPapers().stream()
            .filter(p -> p.getPubDate() != null && p.getPubDate().getYear() >= currentYear - 2)
            .count();
    }
}