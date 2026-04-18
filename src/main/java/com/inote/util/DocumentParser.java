// 声明当前源文件所属包。
package com.inote.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为通用组件。
@Component
// 定义文档解析工具，负责按文件类型提取文本并切分内容。
public class DocumentParser {

    /**
     * 根据上传文件扩展名选择对应的文本解析策略。
     * @param file 文件参数。
     * @return 处理后的字符串结果。
     * @throws IOException 文件读写失败时抛出。
     */
    public String parse(MultipartFile file) throws IOException {
        // 计算并保存originalfilename结果。
        String originalFilename = file.getOriginalFilename();
        // 根据条件判断当前分支是否执行。
        if (originalFilename == null) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 计算并保存extension结果。
        String extension = getFileExtension(originalFilename).toLowerCase();

        // 根据不同条件分派后续处理逻辑。
        switch (extension) {
            // 处理当前分支对应的匹配情况。
            case "pdf":
                // 返回 `parsePdf` 的处理结果。
                return parsePdf(file);
            // 处理当前分支对应的匹配情况。
            case "docx":
            // 处理当前分支对应的匹配情况。
            case "doc":
                // 返回 `parseWord` 的处理结果。
                return parseWord(file);
            // 处理当前分支对应的匹配情况。
            case "xlsx":
            // 处理当前分支对应的匹配情况。
            case "xls":
                // 返回 `parseExcel` 的处理结果。
                return parseExcel(file);
            // 处理当前分支对应的匹配情况。
            case "txt":
            // 处理当前分支对应的匹配情况。
            case "csv":
                // 返回 `parseText` 的处理结果。
                return parseText(file);
            // 处理未命中任何分支时的默认情况。
            default:
                // 抛出 `IllegalArgumentException` 异常中断当前流程。
                throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }
    }

    /**
     * 处理parsepdf相关逻辑。
     * @param file 文件参数。
     * @return 处理后的字符串结果。
     * @throws IOException 文件读写失败时抛出。
     */
    private String parsePdf(MultipartFile file) throws IOException {
        // 记录当前流程的运行日志。
        log.debug("Parsing PDF file: {}", file.getOriginalFilename());
        // 进入异常保护块执行关键逻辑。
        try (InputStream is = file.getInputStream()) {
            // 计算并保存pdfbytes结果。
            byte[] pdfBytes = is.readAllBytes();
            // 进入异常保护块执行关键逻辑。
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                // 创建stripper对象。
                PDFTextStripper stripper = new PDFTextStripper();
                // 返回 `getText` 的处理结果。
                return stripper.getText(document);
            }
        }
    }

    /**
     * 处理parseword相关逻辑。
     * @param file 文件参数。
     * @return 处理后的字符串结果。
     * @throws IOException 文件读写失败时抛出。
     */
    private String parseWord(MultipartFile file) throws IOException {
        // 记录当前流程的运行日志。
        log.debug("Parsing Word file: {}", file.getOriginalFilename());
        // 进入异常保护块执行关键逻辑。
        try (InputStream is = file.getInputStream();
             // 围绕xwpfdocument文档xwpfdocument补充当前业务语句。
             XWPFDocument document = new XWPFDocument(is)) {
            // 计算并保存paragraphs结果。
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            // 返回 `stream` 的处理结果。
            return paragraphs.stream()
                    // 设置map字段的取值。
                    .map(XWPFParagraph::getText)
                    // 设置collect字段的取值。
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 处理parseexcel相关逻辑。
     * @param file 文件参数。
     * @return 处理后的字符串结果。
     * @throws IOException 文件读写失败时抛出。
     */
    private String parseExcel(MultipartFile file) throws IOException {
        // 记录当前流程的运行日志。
        log.debug("Parsing Excel file: {}", file.getOriginalFilename());
        // 创建内容对象。
        StringBuilder content = new StringBuilder();

        // 进入异常保护块执行关键逻辑。
        try (InputStream is = file.getInputStream();
             // 围绕workbookworkbookcreate补充当前业务语句。
             Workbook workbook = createWorkbook(is, file.getOriginalFilename())) {

            // 遍历当前集合或区间中的元素。
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                // 计算并保存sheet结果。
                Sheet sheet = workbook.getSheetAt(i);
                // 调用 `append` 完成当前步骤。
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                // 遍历当前集合或区间中的元素。
                for (Row row : sheet) {
                    // 创建row内容对象。
                    StringBuilder rowContent = new StringBuilder();
                    // 遍历当前集合或区间中的元素。
                    for (Cell cell : row) {
                        // 计算并保存cellvalue结果。
                        String cellValue = getCellValueAsString(cell);
                        // 根据条件判断当前分支是否执行。
                        if (!cellValue.isEmpty()) {
                            // 调用 `append` 完成当前步骤。
                            rowContent.append(cellValue).append("\t");
                        }
                    }
                    // 根据条件判断当前分支是否执行。
                    if (rowContent.length() > 0) {
                        // 调用 `append` 完成当前步骤。
                        content.append(rowContent.toString().trim()).append("\n");
                    }
                }
                // 调用 `append` 完成当前步骤。
                content.append("\n");
            }
        }

        // 返回 `toString` 的处理结果。
        return content.toString().trim();
    }

    /**
     * 处理createworkbook相关逻辑。
     * @param is is参数。
     * @param filename filename参数。
     * @return workbook结果。
     * @throws IOException 文件读写失败时抛出。
     */
    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        // 计算并保存extension结果。
        String extension = getFileExtension(filename).toLowerCase();
        // 根据条件判断当前分支是否执行。
        if ("xlsx".equals(extension)) {
            // 返回 `XSSFWorkbook` 的处理结果。
            return new XSSFWorkbook(is);
        } else {
            // 返回 `HSSFWorkbook` 的处理结果。
            return new HSSFWorkbook(is);
        }
    }

    /**
     * 处理getcellvalueasstring相关逻辑。
     * @param cell cell参数。
     * @return 处理后的字符串结果。
     */
    private String getCellValueAsString(Cell cell) {
        // 根据条件判断当前分支是否执行。
        if (cell == null) {
            // 返回""。
            return "";
        }

        // 根据不同条件分派后续处理逻辑。
        switch (cell.getCellType()) {
            // 处理当前分支对应的匹配情况。
            case STRING:
                // 返回 `getStringCellValue` 的处理结果。
                return cell.getStringCellValue();
            // 处理当前分支对应的匹配情况。
            case NUMERIC:
                // 根据条件判断当前分支是否执行。
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 返回 `getDateCellValue` 的处理结果。
                    return cell.getDateCellValue().toString();
                }
                // 返回 `valueOf` 的处理结果。
                return String.valueOf(cell.getNumericCellValue());
            // 处理当前分支对应的匹配情况。
            case BOOLEAN:
                // 返回 `valueOf` 的处理结果。
                return String.valueOf(cell.getBooleanCellValue());
            // 处理当前分支对应的匹配情况。
            case FORMULA:
                // 返回 `getCellFormula` 的处理结果。
                return cell.getCellFormula();
            // 处理未命中任何分支时的默认情况。
            default:
                // 返回""。
                return "";
        }
    }

    /**
     * 处理parsetext相关逻辑。
     * @param file 文件参数。
     * @return 处理后的字符串结果。
     * @throws IOException 文件读写失败时抛出。
     */
    private String parseText(MultipartFile file) throws IOException {
        // 记录当前流程的运行日志。
        log.debug("Parsing text file: {}", file.getOriginalFilename());
        // 进入异常保护块执行关键逻辑。
        try (InputStream is = file.getInputStream();
             // 围绕bufferedreaderreader补充当前业务语句。
             BufferedReader reader = new BufferedReader(
                     // 围绕输入streamreader补充当前业务语句。
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // 返回 `lines` 的处理结果。
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 处理get文件extension相关逻辑。
     * @param filename filename参数。
     * @return 处理后的字符串结果。
     */
    private String getFileExtension(String filename) {
        // 计算并保存lastdotindex结果。
        int lastDotIndex = filename.lastIndexOf('.');
        // 根据条件判断当前分支是否执行。
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回""。
            return "";
        }
        // 返回 `substring` 的处理结果。
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 按段落和重叠窗口切分长文本，生成可检索分块。
     * @param text text参数。
     * @param chunkSize 分块size参数。
     * @param overlap overlap参数。
     * @return 列表形式的处理结果。
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        // 根据条件判断当前分支是否执行。
        if (text == null || text.isEmpty()) {
            // 返回固定列表结果。
            return List.of();
        }

        // 计算并保存paragraphs结果。
        String[] paragraphs = text.split("\n\\s*\n");

        // 创建分块对象。
        java.util.List<String> chunks = new java.util.ArrayList<>();
        // 创建当前分块对象。
        StringBuilder currentChunk = new StringBuilder();

        // 遍历当前集合或区间中的元素。
        for (String paragraph : paragraphs) {
            // 清理并规范化paragraph内容。
            paragraph = paragraph.trim();
            // 根据条件判断当前分支是否执行。
            if (paragraph.isEmpty()) {
                // 跳过当前循环剩余逻辑，进入下一轮迭代。
                continue;
            }

            // 根据条件判断当前分支是否执行。
            if (currentChunk.length() + paragraph.length() > chunkSize
                    && currentChunk.length() > 0) {
                // 向当前集合中追加元素。
                chunks.add(currentChunk.toString().trim());

                // 计算并保存分块text结果。
                String chunkText = currentChunk.toString();
                // 根据条件判断当前分支是否执行。
                if (chunkText.length() > overlap) {
                    // 围绕当前分块builder补充当前业务语句。
                    currentChunk = new StringBuilder(
                            // 调用 `substring` 完成当前步骤。
                            chunkText.substring(chunkText.length() - overlap));
                } else {
                    // 创建当前分块对象。
                    currentChunk = new StringBuilder();
                }
            }

            // 调用 `append` 完成当前步骤。
            currentChunk.append(paragraph).append("\n\n");
        }

        // 根据条件判断当前分支是否执行。
        if (currentChunk.length() > 0) {
            // 向当前集合中追加元素。
            chunks.add(currentChunk.toString().trim());
        }

        // 返回分块。
        return chunks;
    }
}
