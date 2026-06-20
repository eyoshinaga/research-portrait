package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Researcher;
import com.jlvtc.researchportrait.service.ResearcherService;
import com.jlvtc.researchportrait.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 科研人员 接口
 */
@RestController
@RequestMapping("/api/researcher")
public class ResearcherController {

    @Autowired
    private ResearcherService researcherService;

    /**
     * 添加/更新科研人员
     */
    @PostMapping("/save")
    public Researcher save(@RequestBody Researcher researcher) {
        return researcherService.saveResearcher(researcher);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/get/{id}")
    public Researcher getById(@PathVariable Long id) {
        return researcherService.findById(id);
    }

    /**
     * 查询所有
     */
    @GetMapping("/all")
    public List<Researcher> getAll() {
        return researcherService.findAll();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        researcherService.deleteById(id);
        return "删除成功";
    }

    /**
     * 获取画像详情（含趋势和影响力）
     */
    @GetMapping("/profile/{id}")
    public Map<String, Object> getProfile(@PathVariable Long id) {
        return researcherService.getProfileDetail(id);
    }

    /**
     * 导出科研人员数据为 Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            List<Researcher> researchers = researcherService.findAll();
            return buildExcelResponse(researchers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 高级人才筛选
     */
    @PostMapping("/search")
    public List<Researcher> search(@RequestBody Map<String, Object> criteria) {
        return researcherService.advancedSearch(criteria);
    }

    /**
     * 导出筛选结果
     */
    @PostMapping("/export-search")
    public ResponseEntity<byte[]> exportSearchResult(@RequestBody Map<String, Object> criteria) {
        try {
            List<Researcher> researchers = researcherService.advancedSearch(criteria);
            return buildExcelResponse(researchers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动触发：刷新单个科研人员影响力指数
     */
    @PostMapping("/refresh-influence/{id}")
    public Map<String, Object> refreshInfluence(@PathVariable Long id) {
        Researcher updated = researcherService.refreshInfluenceIndex(id);
        Map<String, Object> resp = new LinkedHashMap<>();
        if (updated != null) {
            resp.put("success", true);
            resp.put("id", updated.getId());
            resp.put("name", updated.getName());
            resp.put("influenceIndex", updated.getInfluenceIndex());
        } else {
            resp.put("success", false);
            resp.put("message", "未找到科研人员 id=" + id);
        }
        return resp;
    }

    /**
     * 手动触发：全量刷新所有科研人员影响力指数
     */
    @PostMapping("/refresh-influence/all")
    public Map<String, Object> refreshAllInfluence() {
        long start = System.currentTimeMillis();
        int count = researcherService.refreshAllInfluenceIndex();
        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("refreshedCount", count);
        resp.put("elapsedMs", elapsed);
        return resp;
    }

    /**
     * 下载科研人员智能分析报告（Markdown 格式）
     */
    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        String report = researcherService.generateAnalysisReport(id);
        byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "researcher_report_" + id + ".md");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private ResponseEntity<byte[]> buildExcelResponse(List<Researcher> researchers) throws Exception {
        List<String> headers = Arrays.asList("姓名", "职称", "所属院系", "研究方向", "影响力指数");
        List<Map<String, Object>> dataList = researchers.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("姓名", r.getName());
            map.put("职称", r.getTitle());
            map.put("所属院系", r.getDepartment());
            map.put("研究方向", r.getResearchField());
            map.put("影响力指数", r.getInfluenceIndex());
            return map;
        }).collect(Collectors.toList());

        byte[] excelData = ExcelExportUtil.exportToExcel(headers, dataList);
        HttpHeaders headersResp = new HttpHeaders();
        headersResp.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headersResp.setContentDispositionFormData("attachment", "talent_search_result.xlsx");
        return ResponseEntity.ok().headers(headersResp).body(excelData);
    }
}