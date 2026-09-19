package com.jlvtc.researchportrait.service;

import com.jlvtc.researchportrait.entity.WeightConfig;
import com.jlvtc.researchportrait.repository.WeightConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态权重配置 业务层
 * 管理员通过管理界面调整各学科的论文/专利/项目权重及时间衰减系数，
 * 调整结果即时作用于影响力指数计算（无需重启服务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightConfigService {

    private final WeightConfigRepository weightConfigRepo;

    /** 各学科默认权重模板（恢复默认时使用）：[论文权重, 专利权重, 项目权重, 衰减系数] */
    private static final Map<String, double[]> DEFAULT_TEMPLATES = new LinkedHashMap<>();
    static {
        DEFAULT_TEMPLATES.put("工学", new double[]{0.30, 0.40, 0.30, 0.05});
        DEFAULT_TEMPLATES.put("理学", new double[]{0.60, 0.10, 0.30, 0.05});
        DEFAULT_TEMPLATES.put("人文社科", new double[]{0.50, 0.10, 0.40, 0.03});
        DEFAULT_TEMPLATES.put("医学", new double[]{0.40, 0.20, 0.40, 0.04});
        DEFAULT_TEMPLATES.put("管理学", new double[]{0.35, 0.25, 0.40, 0.04});
    }

    /** 查询全部学科权重配置（按 id 排序） */
    public List<WeightConfig> listAll() {
        return weightConfigRepo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /** 更新单个学科的权重配置（管理员后台保存入口） */
    public WeightConfig update(WeightConfig config) {
        if (config.getId() == null) {
            throw new IllegalArgumentException("缺少配置 id，无法定位要更新的学科");
        }
        validate(config);
        WeightConfig saved = weightConfigRepo.save(config);
        log.info("[权重配置] 已更新: discipline={}, paper={}, patent={}, project={}, decay={}",
                saved.getDiscipline(), saved.getPaperWeight(), saved.getPatentWeight(),
                saved.getProjectWeight(), saved.getDecayRate());
        return saved;
    }

    /** 恢复所有学科为默认权重模板（已有学科原地更新，缺失学科补建） */
    public List<WeightConfig> resetToDefaults() {
        List<WeightConfig> all = weightConfigRepo.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Set<String> existing = new HashSet<>();
        long maxId = 0;
        for (WeightConfig c : all) {
            if (c.getId() != null && c.getId() > maxId) {
                maxId = c.getId();
            }
            double[] template = DEFAULT_TEMPLATES.get(c.getDiscipline());
            if (template != null) {
                applyTemplate(c, template);
                weightConfigRepo.save(c);
            }
            existing.add(c.getDiscipline());
        }
        // 数据库中缺失的默认学科，补充新建
        for (Map.Entry<String, double[]> entry : DEFAULT_TEMPLATES.entrySet()) {
            if (!existing.contains(entry.getKey())) {
                WeightConfig c = new WeightConfig();
                c.setId(++maxId);
                c.setDiscipline(entry.getKey());
                applyTemplate(c, entry.getValue());
                weightConfigRepo.save(c);
            }
        }
        log.info("[权重配置] 已恢复默认权重模板");
        return listAll();
    }

    private void applyTemplate(WeightConfig c, double[] template) {
        c.setPaperWeight(template[0]);
        c.setPatentWeight(template[1]);
        c.setProjectWeight(template[2]);
        c.setDecayRate(template[3]);
    }

    /** 校验权重取值范围：三项权重与衰减系数均需在 0~1 之间 */
    private void validate(WeightConfig c) {
        checkRange("论文权重", c.getPaperWeight());
        checkRange("专利权重", c.getPatentWeight());
        checkRange("项目权重", c.getProjectWeight());
        checkRange("时间衰减系数", c.getDecayRate());
    }

    private void checkRange(String name, Double value) {
        if (value == null || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + "必须在 0~1 之间");
        }
    }
}
