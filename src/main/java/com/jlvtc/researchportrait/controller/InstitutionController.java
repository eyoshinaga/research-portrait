package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.service.InstitutionService;
import com.jlvtc.researchportrait.util.PdfExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    @Autowired
    private InstitutionService institutionService;

    /**
     * 查询所有机构列表（简化，用于前端复选框）
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return institutionService.findAllSimple();
    }

    /**
     * 获取机构竞争力雷达图数据
     */
    @GetMapping("/radar/{id}")
    public Map<String, Object> getRadar(@PathVariable Long id) {
        return institutionService.getCompetitivenessRadar(id);
    }

    /**
     * 多机构竞争力对比
     */
    @PostMapping("/compare")
    public List<Map<String, Object>> compare(@RequestBody List<Long> ids) {
        return institutionService.compareInstitutions(ids);
    }

    /**
     * 雷达图维度钻取：查看构成该维度的二级指标、计算方式说明与原始成果清单
     */
    @GetMapping("/drill/{id}")
    public Map<String, Object> drill(@PathVariable Long id, @RequestParam String dimension) {
        return institutionService.drillDown(id, dimension);
    }

    /**
     * 下载机构竞争力分析简报（PDF 格式，由 Markdown 简报渲染生成）
     */
    @GetMapping("/brief-pdf")
    public ResponseEntity<byte[]> briefPdf(@RequestParam List<Long> ids) {
        try {
            String brief = institutionService.generateBrief(ids);
            byte[] pdfBytes = PdfExportUtil.markdownToPdf(brief);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "institution_competitiveness_brief.pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}