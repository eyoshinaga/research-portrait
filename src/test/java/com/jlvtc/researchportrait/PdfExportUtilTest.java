package com.jlvtc.researchportrait;

import com.jlvtc.researchportrait.util.PdfExportUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PdfExportUtil 单元测试：
 * 验证科研分析报告（Markdown）能正常渲染为 PDF，不依赖 Spring 容器与数据库。
 * 运行方式：mvnw test -Dtest=PdfExportUtilTest
 */
class PdfExportUtilTest {

    @Test
    void markdownToPdfShouldGenerateValidPdf() throws Exception {
        // 模拟真实报告结构：标题、粗体、分隔线、二级/三级标题、表格、列表、斜体
        String markdown = "# 科研人员智能分析报告\n\n"
                + "**姓名**: 张三　　**生成时间**: 2026-09-18\n\n"
                + "---\n\n"
                + "## 一、基本信息\n\n"
                + "| 项目 | 内容 |\n"
                + "|:---|:---|\n"
                + "| 姓名 | 张三 |\n"
                + "| 所属院系 | 汽车工程学院 |\n"
                + "| 学术影响力指数 | 88.50 |\n\n"
                + "## 二、科研产出统计\n\n"
                + "- 论文总数: **12** 篇\n"
                + "- 专利总数: **3** 项\n"
                + "- 主持项目: **2** 项\n\n"
                + "### 论文清单\n\n"
                + "| 序号 | 论文标题 | 期刊 | 被引次数 |\n"
                + "|:---|:---|:---|:---|\n"
                + "| 1 | 智能网联汽车关键技术研究 | 汽车工程 | 15 |\n"
                + "| 2 | 新能源汽车电池管理系统设计 | 电源技术 | 8 |\n\n"
                + "## 三、综合评价\n\n"
                + "该科研人员在智能网联汽车领域具有较大学术影响力。\n\n"
                + "*本报告由系统自动生成，仅供参考。*\n";

        byte[] pdf = PdfExportUtil.markdownToPdf(markdown);

        // 校验 PDF 文件头（%PDF）与基本大小
        assertTrue(pdf.length > 1000, "PDF 字节数异常: " + pdf.length);
        String fileHeader = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", fileHeader, "PDF 文件头不正确");

        // 输出样例文件，便于人工打开检查排版效果
        Path out = Path.of("target", "pdf-export-sample.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
        System.out.println("[PdfExportUtilTest] PDF 生成成功，大小=" + pdf.length + " 字节，样例文件: " + out.toAbsolutePath());
    }
}
