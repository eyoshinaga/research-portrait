package com.jlvtc.researchportrait.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 导出工具类
 * 将 Markdown 格式的科研分析报告渲染为 PDF 文档：
 * 支持标题（一/二/三级）、表格、列表、行内强调（**内容**）、分隔线与中文排版。
 * 中文字体优先子集化嵌入系统中文字体（任何 PDF 阅读器均可正确显示），系统字体不可用时回退 Adobe 标准 CJK 字体
 */
public class PdfExportUtil {

    /** 中文字体（类加载时初始化一次） */
    private static final BaseFont CN_FONT;

    /** 候选系统中文字体路径（按优先级排列，文件不存在的自动跳过）：Windows / Linux 常见中文字体 */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc,0",                             // Windows 宋体
            "C:/Windows/Fonts/msyh.ttc,0",                               // Windows 微软雅黑
            "C:/Windows/Fonts/simhei.ttf",                               // Windows 黑体
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",  // Linux Noto CJK
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0"           // Linux 文泉驿微米黑
    };

    static {
        CN_FONT = createChineseFont();
    }

    /**
     * 创建中文字体：优先子集化嵌入系统中文字体，确保在任意 PDF 阅读器中都能正确显示中文；
     * 系统中文字体全部不可用时，回退为 itext-asian 提供的 Adobe 标准 CJK 字体 STSong-Light
     */
    private static BaseFont createChineseFont() {
        for (String candidate : FONT_CANDIDATES) {
            // 候选路径可能带 TTC 索引（如 simsun.ttc,0），判断存在性时取逗号前的文件路径
            String filePath = candidate.split(",")[0];
            if (!Files.exists(Path.of(filePath))) {
                continue;
            }
            try {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, null, null, true);
            } catch (Exception e) {
                // 当前候选字体加载失败时继续尝试下一个
            }
        }
        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("初始化中文 PDF 字体失败: " + e.getMessage());
        }
    }

    // ============ 各类文本字体样式 ============
    private static final Font FONT_TITLE      = new Font(CN_FONT, 18, Font.NORMAL, new BaseColor(31, 78, 121));
    private static final Font FONT_H2         = new Font(CN_FONT, 13.5f, Font.NORMAL, new BaseColor(31, 78, 121));
    private static final Font FONT_H3         = new Font(CN_FONT, 11.5f, Font.NORMAL, new BaseColor(64, 64, 64));
    private static final Font FONT_BODY       = new Font(CN_FONT, 10.5f, Font.NORMAL, new BaseColor(51, 51, 51));
    private static final Font FONT_EMPH       = new Font(CN_FONT, 10.5f, Font.NORMAL, new BaseColor(192, 0, 0));
    private static final Font FONT_TABLE_HEAD = new Font(CN_FONT, 10, Font.NORMAL, new BaseColor(31, 78, 121));
    private static final Font FONT_TABLE_CELL = new Font(CN_FONT, 9.5f, Font.NORMAL, new BaseColor(51, 51, 51));
    private static final Font FONT_NOTE       = new Font(CN_FONT, 9, Font.NORMAL, new BaseColor(128, 128, 128));

    private static final BaseColor TABLE_BORDER = new BaseColor(190, 200, 210);
    private static final BaseColor TABLE_HEAD_BG = new BaseColor(240, 244, 249);

    /**
     * 将 Markdown 文本渲染为 PDF 字节数组
     */
    public static byte[] markdownToPdf(String markdown) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 56, 56, 64, 56);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PageFooterEvent());
        document.open();

        // 表格行缓冲区（连续的 | 行组成一张表格）
        List<String[]> tableBuffer = new ArrayList<>();

        for (String rawLine : markdown.split("\n", -1)) {
            String line = rawLine.strip();

            // 表格行：先收集，遇到非表格行再统一输出
            if (line.startsWith("|")) {
                tableBuffer.add(splitTableRow(line));
                continue;
            }
            if (!tableBuffer.isEmpty()) {
                flushTable(document, tableBuffer);
                tableBuffer.clear();
            }

            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("---")) {
                document.add(new Chunk(new LineSeparator(0.6f, 100, new BaseColor(190, 200, 210), Element.ALIGN_CENTER, -2)));
                continue;
            }
            if (line.startsWith("### ")) {
                document.add(heading(line.substring(4), FONT_H3, 10, 4));
                continue;
            }
            if (line.startsWith("## ")) {
                document.add(heading(line.substring(3), FONT_H2, 14, 6));
                continue;
            }
            if (line.startsWith("# ")) {
                Paragraph p = new Paragraph(line.substring(2), FONT_TITLE);
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingAfter(4);
                document.add(p);
                continue;
            }
            if (line.startsWith("- ")) {
                Paragraph p = richParagraph("· " + line.substring(2), FONT_BODY);
                p.setIndentationLeft(8);
                p.setSpacingAfter(2);
                document.add(p);
                continue;
            }
            // 整行斜体说明（*内容*）：居中灰色小字
            if (line.startsWith("*") && line.endsWith("*") && line.length() > 2) {
                Paragraph p = new Paragraph(line.substring(1, line.length() - 1), FONT_NOTE);
                p.setAlignment(Element.ALIGN_CENTER);
                document.add(p);
                continue;
            }
            // 普通段落（支持行内 **强调**）
            Paragraph p = richParagraph(line, FONT_BODY);
            p.setSpacingAfter(5);
            document.add(p);
        }
        if (!tableBuffer.isEmpty()) {
            flushTable(document, tableBuffer);
        }

        document.close();
        return baos.toByteArray();
    }

    /** 构建标题段落 */
    private static Paragraph heading(String text, Font font, float spaceBefore, float spaceAfter) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        return p;
    }

    /** 构建支持行内 **强调** 的段落 */
    private static Paragraph richParagraph(String text, Font baseFont) {
        Paragraph p = new Paragraph();
        String[] segments = text.split("\\*\\*");
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].isEmpty()) {
                continue;
            }
            p.add(new Chunk(segments[i], i % 2 == 1 ? FONT_EMPH : baseFont));
        }
        return p;
    }

    /** 拆分表格行 "| a | b |" 为单元格数组 */
    private static String[] splitTableRow(String line) {
        String[] parts = line.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (int i = 1; i < parts.length - 1; i++) {
            cells.add(parts[i].strip());
        }
        return cells.toArray(new String[0]);
    }

    /** 将收集的表格行渲染为 PDF 表格（首行为表头，`:---` 分隔行自动忽略，表头跨页重复） */
    private static void flushTable(Document document, List<String[]> rows) throws DocumentException {
        List<String[]> dataRows = new ArrayList<>();
        for (String[] row : rows) {
            // 过滤 Markdown 分隔行（如 |:---|:---|）
            if (row.length > 0 && row[0].matches(":?-{2,}:?")) {
                continue;
            }
            dataRows.add(row);
        }
        if (dataRows.isEmpty()) {
            return;
        }

        int columns = dataRows.get(0).length;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);
        table.setHeaderRows(1);

        for (int r = 0; r < dataRows.size(); r++) {
            String[] row = dataRows.get(r);
            for (int c = 0; c < columns; c++) {
                String text = c < row.length ? row[c] : "";
                PdfPCell cell = new PdfPCell(new Phrase(text.replace("**", ""), r == 0 ? FONT_TABLE_HEAD : FONT_TABLE_CELL));
                cell.setPadding(4);
                cell.setMinimumHeight(16);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBorderColor(TABLE_BORDER);
                if (r == 0) {
                    cell.setBackgroundColor(TABLE_HEAD_BG);
                }
                table.addCell(cell);
            }
        }
        document.add(table);
    }

    /** 页脚事件：在每页底部居中显示页码 */
    private static class PageFooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                Phrase footer = new Phrase("第 " + writer.getPageNumber() + " 页", FONT_NOTE);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                        (document.left() + document.right()) / 2, document.bottom() - 22, 0);
            } catch (Exception ignored) {
                // 页脚绘制失败不影响正文导出
            }
        }
    }
}
