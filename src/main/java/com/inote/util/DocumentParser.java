// 声明包路径，工具层
package com.inote.util;

// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 PDFBox 加载器，用于加载 PDF 文件
import org.apache.pdfbox.Loader;
// 导入 PDFBox 随机访问读取缓冲区
import org.apache.pdfbox.io.RandomAccessReadBuffer;
// 导入 PDFBox PDF 文档模型
import org.apache.pdfbox.pdmodel.PDDocument;
// 导入 PDFBox 文本提取器
import org.apache.pdfbox.text.PDFTextStripper;
// 导入 Apache POI 旧版 Excel（.xls）工作簿
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
// 导入 Apache POI 单元格接口
import org.apache.poi.ss.usermodel.Cell;
// 导入 Apache POI 日期工具，判断单元格是否为日期格式
import org.apache.poi.ss.usermodel.DateUtil;
// 导入 Apache POI 行接口
import org.apache.poi.ss.usermodel.Row;
// 导入 Apache POI 工作表接口
import org.apache.poi.ss.usermodel.Sheet;
// 导入 Apache POI 工作簿接口
import org.apache.poi.ss.usermodel.Workbook;
// 导入 Apache POI 新版 Excel（.xlsx）工作簿
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// 导入 Apache POI Word（.docx）文档模型
import org.apache.poi.xwpf.usermodel.XWPFDocument;
// 导入 Apache POI Word 段落对象
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
// 导入 Spring 组件注解
import org.springframework.stereotype.Component;
// 导入文件上传接口
import org.springframework.web.multipart.MultipartFile;

// 导入缓冲读取器
import java.io.BufferedReader;
// 导入 IO 异常类
import java.io.IOException;
// 导入输入流接口
import java.io.InputStream;
// 导入字节流转字符流的桥接器
import java.io.InputStreamReader;
// 导入标准字符集，使用 UTF-8
import java.nio.charset.StandardCharsets;
// 导入 List 集合接口
import java.util.List;
// 导入流式收集器
import java.util.stream.Collectors;

/**
 * 文档解析工具类
 * 支持解析 PDF、Word（.docx）、Excel（.xlsx/.xls）、TXT、CSV 格式的文件
 * 并提供文本分块功能，用于后续的向量化处理
 */
// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 组件，纳入 Spring 容器管理
@Component
// 文档解析器，将各种格式的文件解析为纯文本
public class DocumentParser {

    /**
     * 根据文件扩展名自动选择解析器，解析文档内容
     * @param file 上传的文件
     * @return 提取出的纯文本内容
     */
    public String parse(MultipartFile file) throws IOException {
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 检查文件名是否为 null
        if (originalFilename == null) {
            // 抛出非法参数异常
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 提取文件扩展名并转为小写
        String extension = getFileExtension(originalFilename).toLowerCase();

        // 根据扩展名选择对应的解析方法
        switch (extension) {
            // PDF 格式，使用 PDFBox 解析
            case "pdf":
                return parsePdf(file);
            // Word 新格式
            case "docx":
            // Word 旧格式（注意：实际只支持 .docx，.doc 需要额外处理）
            case "doc":
                // 使用 Apache POI 解析
                return parseWord(file);
            // Excel 新格式
            case "xlsx":
            // Excel 旧格式
            case "xls":
                // 使用 Apache POI 解析
                return parseExcel(file);
            // 纯文本格式
            case "txt":
            // CSV 逗号分隔值格式
            case "csv":
                // 直接按 UTF-8 读取文本内容
                return parseText(file);
            // 不支持的格式
            default:
                // 抛出异常
                throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }
    }

    /**
     * 解析 PDF 文件，提取所有页面的文本内容
     * 使用 Apache PDFBox 库
     */
    private String parsePdf(MultipartFile file) throws IOException {
        // 记录正在解析的文件名
        log.debug("Parsing PDF file: {}", file.getOriginalFilename());
        // 获取文件输入流，try-with-resources 自动关闭
        try (InputStream is = file.getInputStream()) {
            // 将 PDF 文件全部读入内存字节数组
            byte[] pdfBytes = is.readAllBytes();
            // 用 PDFBox 加载 PDF 文档，自动关闭
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                // 创建 PDF 文本提取器
                PDFTextStripper stripper = new PDFTextStripper();
                // 提取 PDF 中所有页面的文本内容并返回
                return stripper.getText(document);
            }
        }
    }

    /**
     * 解析 Word（.docx）文件，提取所有段落的文本内容
     * 使用 Apache POI 库
     */
    private String parseWord(MultipartFile file) throws IOException {
        // 记录正在解析的文件名
        log.debug("Parsing Word file: {}", file.getOriginalFilename());
        // 获取文件输入流
        try (InputStream is = file.getInputStream();
             // 用 POI 加载 .docx 文档
             XWPFDocument document = new XWPFDocument(is)) {
            // 获取文档中所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            // 将段落列表转为流，提取每个段落的文本内容，用换行符连接
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析 Excel 文件，提取所有工作表、所有行的单元格内容
     * 支持 .xlsx（XSSFWorkbook）和 .xls（HSSFWorkbook）格式
     */
    private String parseExcel(MultipartFile file) throws IOException {
        // 记录正在解析的文件名
        log.debug("Parsing Excel file: {}", file.getOriginalFilename());
        // 创建可变字符串用于拼接内容
        StringBuilder content = new StringBuilder();

        // 获取文件输入流
        try (InputStream is = file.getInputStream();
             // 根据文件格式创建对应的 Workbook 对象
             Workbook workbook = createWorkbook(is, file.getOriginalFilename())) {

            // 遍历所有工作表
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                // 获取第 i 个工作表
                Sheet sheet = workbook.getSheetAt(i);
                // 添加工作表名称
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                // 遍历当前工作表的所有行
                for (Row row : sheet) {
                    // 创建行内容缓冲区
                    StringBuilder rowContent = new StringBuilder();
                    // 遍历当前行的所有单元格
                    for (Cell cell : row) {
                        // 将单元格值转为字符串
                        String cellValue = getCellValueAsString(cell);
                        // 如果单元格不为空
                        if (!cellValue.isEmpty()) {
                            // 用制表符分隔单元格内容
                            rowContent.append(cellValue).append("\t");
                        }
                    }
                    // 如果当前行有内容
                    if (rowContent.length() > 0) {
                        // 去除末尾制表符后添加换行
                        content.append(rowContent.toString().trim()).append("\n");
                    }
                }
                // 工作表之间用空行分隔
                content.append("\n");
            }
        }

        // 去除首尾空白后返回
        return content.toString().trim();
    }

    /**
     * 根据文件扩展名创建对应的 Workbook 对象
     * .xlsx 使用 XSSFWorkbook，.xls 使用 HSSFWorkbook
     */
    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        // 提取扩展名并转小写
        String extension = getFileExtension(filename).toLowerCase();
        // 如果是 .xlsx 格式（Office 2007+）
        if ("xlsx".equals(extension)) {
            // 创建新版 Excel 工作簿
            return new XSSFWorkbook(is);
        } else {
            // 否则是 .xls 格式（Office 97-2003），创建旧版 Excel 工作簿
            return new HSSFWorkbook(is);
        }
    }

    /**
     * 将 Excel 单元格值转为字符串
     * 支持 STRING、NUMERIC（含日期）、BOOLEAN、FORMULA 类型
     */
    private String getCellValueAsString(Cell cell) {
        // 如果单元格为 null
        if (cell == null) {
            // 返回空字符串
            return "";
        }

        // 根据单元格类型提取值
        switch (cell.getCellType()) {
            // 字符串类型
            case STRING:
                // 直接返回字符串值
                return cell.getStringCellValue();
            // 数值类型（含日期）
            case NUMERIC:
                // 判断是否为日期格式
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 返回日期的字符串表示
                    return cell.getDateCellValue().toString();
                }
                // 返回数值的字符串表示
                return String.valueOf(cell.getNumericCellValue());
            // 布尔类型
            case BOOLEAN:
                // 返回 "true" 或 "false"
                return String.valueOf(cell.getBooleanCellValue());
            // 公式类型
            case FORMULA:
                // 返回公式表达式文本
                return cell.getCellFormula();
            // 其他类型（BLANK、ERROR 等）
            default:
                // 返回空字符串
                return "";
        }
    }

    /**
     * 解析纯文本文件（TXT、CSV）
     * 按 UTF-8 编码逐行读取并拼接
     */
    private String parseText(MultipartFile file) throws IOException {
        // 记录正在解析的文件名
        log.debug("Parsing text file: {}", file.getOriginalFilename());
        // 获取文件输入流
        try (InputStream is = file.getInputStream();
             // 创建缓冲读取器，使用 UTF-8 编码将字节流转为字符流
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // 逐行读取并用换行符拼接所有行
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 从文件名中提取扩展名
     */
    private String getFileExtension(String filename) {
        // 查找最后一个点号的位置
        int lastDotIndex = filename.lastIndexOf('.');
        // 如果没有点号或点号在末尾
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回空字符串
            return "";
        }
        // 返回点号之后的扩展名部分
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 将长文本分割成固定大小的文本块
     * 按段落（双换行符）为边界分块，相邻块之间有字符重叠以保持语义连贯
     * @param text 原始文本
     * @param chunkSize 每块的最大字符数
     * @param overlap 相邻块之间的重叠字符数
     * @return 分块后的文本列表
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        // 检查文本是否为 null 或空
        if (text == null || text.isEmpty()) {
            // 返回空的不可变列表
            return List.of();
        }

        // 按双换行符（空行）分割文本为段落数组，正则匹配：换行 + 可选空白 + 换行
        String[] paragraphs = text.split("\n\\s*\n");

        // 创建可变列表存储分块结果
        java.util.List<String> chunks = new java.util.ArrayList<>();
        // 创建当前块的缓冲区
        StringBuilder currentChunk = new StringBuilder();

        // 遍历每个段落
        for (String paragraph : paragraphs) {
            // 去除段落首尾空白
            paragraph = paragraph.trim();
            // 如果段落为空
            if (paragraph.isEmpty()) {
                // 跳过空段落
                continue;
            }

            // 如果当前块加上新段落超过块大小限制，且当前块有内容，则保存当前块
            if (currentChunk.length() + paragraph.length() > chunkSize
                    && currentChunk.length() > 0) {
                // 将当前块的内容保存到结果列表
                chunks.add(currentChunk.toString().trim());

                // 保留重叠部分：取当前块末尾的 overlap 个字符作为下一块的开头
                String chunkText = currentChunk.toString();
                // 如果当前块长度大于重叠大小
                if (chunkText.length() > overlap) {
                    // 新块以当前块末尾的重叠部分开头
                    currentChunk = new StringBuilder(
                            chunkText.substring(chunkText.length() - overlap));
                } else {
                    // 当前块太短则不保留重叠，从空开始
                    currentChunk = new StringBuilder();
                }
            }

            // 将段落添加到当前块，段落间用空行分隔
            currentChunk.append(paragraph).append("\n\n");
        }

        // 如果缓冲区还有剩余内容，添加最后一个未保存的块
        if (currentChunk.length() > 0) {
            // 保存最后一个块
            chunks.add(currentChunk.toString().trim());
        }

        // 返回所有分块结果
        return chunks;
    }
}
