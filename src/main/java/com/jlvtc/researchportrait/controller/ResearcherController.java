package com.jlvtc.researchportrait.controller;

import com.jlvtc.researchportrait.entity.Researcher;
import com.jlvtc.researchportrait.service.ResearcherService;
import com.jlvtc.researchportrait.util.ExcelExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            headersResp.setContentDispositionFormData("attachment", "research_portrait.xlsx");
            
            return ResponseEntity.ok().headers(headersResp).body(excelData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}