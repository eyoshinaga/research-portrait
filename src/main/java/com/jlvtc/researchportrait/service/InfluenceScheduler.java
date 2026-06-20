package com.jlvtc.researchportrait.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 影响力指数定时刷新任务
 * 每天凌晨 2:00 自动全量重算所有科研人员的学术影响力指数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InfluenceScheduler {

    private final ResearcherService researcherService;

    /**
     * 每日凌晨 2:00 执行（cron: 秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledRefreshAllInfluence() {
        log.info("[定时任务] 开始全量刷新影响力指数...");
        try {
            int count = researcherService.refreshAllInfluenceIndex();
            log.info("[定时任务] 影响力指数刷新完成，共处理 {} 名科研人员", count);
        } catch (Exception e) {
            log.error("[定时任务] 影响力指数刷新失败: {}", e.getMessage(), e);
        }
    }
}
