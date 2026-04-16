// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Component
// 声明当前类型。
public class DocumentParser {

    /**
     * 描述 `parse` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `String` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    public String parse(MultipartFile file) throws IOException {
        // 执行当前语句。
        String originalFilename = file.getOriginalFilename();
        // 执行当前流程控制分支。
        if (originalFilename == null) {
            // 抛出当前异常。
            throw new IllegalArgumentException("文件名不能为空");
        // 结束当前代码块。
        }

        // 执行当前语句。
        String extension = getFileExtension(originalFilename).toLowerCase();

        // 执行当前流程控制分支。
        switch (extension) {
            // 执行当前流程控制分支。
            case "pdf":
                // 返回当前结果。
                return parsePdf(file);
            // 执行当前流程控制分支。
            case "docx":
            // 执行当前流程控制分支。
            case "doc":
                // 返回当前结果。
                return parseWord(file);
            // 执行当前流程控制分支。
            case "xlsx":
            // 执行当前流程控制分支。
            case "xls":
                // 返回当前结果。
                return parseExcel(file);
            // 执行当前流程控制分支。
            case "txt":
            // 执行当前流程控制分支。
            case "csv":
                // 返回当前结果。
                return parseText(file);
            // 执行当前流程控制分支。
            default:
                // 抛出当前异常。
                throw new IllegalArgumentException("不支持的文件格式: " + extension);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `parsePdf` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `String` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    private String parsePdf(MultipartFile file) throws IOException {
        // 执行当前语句。
        log.debug("Parsing PDF file: {}", file.getOriginalFilename());
        // 执行当前流程控制分支。
        try (InputStream is = file.getInputStream()) {
            // 执行当前语句。
            byte[] pdfBytes = is.readAllBytes();
            // 执行当前流程控制分支。
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                // 执行当前语句。
                PDFTextStripper stripper = new PDFTextStripper();
                // 返回当前结果。
                return stripper.getText(document);
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `parseWord` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `String` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    private String parseWord(MultipartFile file) throws IOException {
        // 执行当前语句。
        log.debug("Parsing Word file: {}", file.getOriginalFilename());
        // 执行当前流程控制分支。
        try (InputStream is = file.getInputStream();
             // 处理当前代码结构。
             XWPFDocument document = new XWPFDocument(is)) {
            // 执行当前语句。
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            // 返回当前结果。
            return paragraphs.stream()
                    // 处理当前代码结构。
                    .map(XWPFParagraph::getText)
                    // 执行当前语句。
                    .collect(Collectors.joining("\n"));
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `parseExcel` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `String` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    private String parseExcel(MultipartFile file) throws IOException {
        // 执行当前语句。
        log.debug("Parsing Excel file: {}", file.getOriginalFilename());
        // 执行当前语句。
        StringBuilder content = new StringBuilder();

        // 执行当前流程控制分支。
        try (InputStream is = file.getInputStream();
             // 处理当前代码结构。
             Workbook workbook = createWorkbook(is, file.getOriginalFilename())) {

            // 执行当前流程控制分支。
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                // 执行当前语句。
                Sheet sheet = workbook.getSheetAt(i);
                // 执行当前语句。
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                // 执行当前流程控制分支。
                for (Row row : sheet) {
                    // 执行当前语句。
                    StringBuilder rowContent = new StringBuilder();
                    // 执行当前流程控制分支。
                    for (Cell cell : row) {
                        // 执行当前语句。
                        String cellValue = getCellValueAsString(cell);
                        // 执行当前流程控制分支。
                        if (!cellValue.isEmpty()) {
                            // 执行当前语句。
                            rowContent.append(cellValue).append("\t");
                        // 结束当前代码块。
                        }
                    // 结束当前代码块。
                    }
                    // 执行当前流程控制分支。
                    if (rowContent.length() > 0) {
                        // 执行当前语句。
                        content.append(rowContent.toString().trim()).append("\n");
                    // 结束当前代码块。
                    }
                // 结束当前代码块。
                }
                // 执行当前语句。
                content.append("\n");
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }

        // 返回当前结果。
        return content.toString().trim();
    // 结束当前代码块。
    }

    /**
     * 描述 `createWorkbook` 操作。
     *
     * @param is 输入参数 `is`。
     * @param filename 输入参数 `filename`。
     * @return 类型为 `Workbook` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        // 执行当前语句。
        String extension = getFileExtension(filename).toLowerCase();
        // 执行当前流程控制分支。
        if ("xlsx".equals(extension)) {
            // 返回当前结果。
            return new XSSFWorkbook(is);
        // 处理当前代码结构。
        } else {
            // 返回当前结果。
            return new HSSFWorkbook(is);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `getCellValueAsString` 操作。
     *
     * @param cell 输入参数 `cell`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String getCellValueAsString(Cell cell) {
        // 执行当前流程控制分支。
        if (cell == null) {
            // 返回当前结果。
            return "";
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        switch (cell.getCellType()) {
            // 执行当前流程控制分支。
            case STRING:
                // 返回当前结果。
                return cell.getStringCellValue();
            // 执行当前流程控制分支。
            case NUMERIC:
                // 执行当前流程控制分支。
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 返回当前结果。
                    return cell.getDateCellValue().toString();
                // 结束当前代码块。
                }
                // 返回当前结果。
                return String.valueOf(cell.getNumericCellValue());
            // 执行当前流程控制分支。
            case BOOLEAN:
                // 返回当前结果。
                return String.valueOf(cell.getBooleanCellValue());
            // 执行当前流程控制分支。
            case FORMULA:
                // 返回当前结果。
                return cell.getCellFormula();
            // 执行当前流程控制分支。
            default:
                // 返回当前结果。
                return "";
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `parseText` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `String` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    private String parseText(MultipartFile file) throws IOException {
        // 执行当前语句。
        log.debug("Parsing text file: {}", file.getOriginalFilename());
        // 执行当前流程控制分支。
        try (InputStream is = file.getInputStream();
             // 处理当前代码结构。
             BufferedReader reader = new BufferedReader(
                     // 处理当前代码结构。
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // 返回当前结果。
            return reader.lines().collect(Collectors.joining("\n"));
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `getFileExtension` 操作。
     *
     * @param filename 输入参数 `filename`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String getFileExtension(String filename) {
        // 执行当前语句。
        int lastDotIndex = filename.lastIndexOf('.');
        // 执行当前流程控制分支。
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回当前结果。
            return "";
        // 结束当前代码块。
        }
        // 返回当前结果。
        return filename.substring(lastDotIndex + 1);
    // 结束当前代码块。
    }

    /**
     * 描述 `chunkText` 操作。
     *
     * @param text 输入参数 `text`。
     * @param chunkSize 输入参数 `chunkSize`。
     * @param overlap 输入参数 `overlap`。
     * @return 类型为 `List<String>` 的返回值。
     */
    // 处理当前代码结构。
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        // 执行当前流程控制分支。
        if (text == null || text.isEmpty()) {
            // 返回当前结果。
            return List.of();
        // 结束当前代码块。
        }

        // 执行当前语句。
        String[] paragraphs = text.split("\n\\s*\n");

        // 执行当前语句。
        java.util.List<String> chunks = new java.util.ArrayList<>();
        // 执行当前语句。
        StringBuilder currentChunk = new StringBuilder();

        // 执行当前流程控制分支。
        for (String paragraph : paragraphs) {
            // 执行当前语句。
            paragraph = paragraph.trim();
            // 执行当前流程控制分支。
            if (paragraph.isEmpty()) {
                // 执行当前语句。
                continue;
            // 结束当前代码块。
            }

            // 执行当前流程控制分支。
            if (currentChunk.length() + paragraph.length() > chunkSize
                    // 处理当前代码结构。
                    && currentChunk.length() > 0) {
                // 执行当前语句。
                chunks.add(currentChunk.toString().trim());

                // 执行当前语句。
                String chunkText = currentChunk.toString();
                // 执行当前流程控制分支。
                if (chunkText.length() > overlap) {
                    // 处理当前代码结构。
                    currentChunk = new StringBuilder(
                            // 执行当前语句。
                            chunkText.substring(chunkText.length() - overlap));
                // 处理当前代码结构。
                } else {
                    // 执行当前语句。
                    currentChunk = new StringBuilder();
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }

            // 执行当前语句。
            currentChunk.append(paragraph).append("\n\n");
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        if (currentChunk.length() > 0) {
            // 执行当前语句。
            chunks.add(currentChunk.toString().trim());
        // 结束当前代码块。
        }

        // 返回当前结果。
        return chunks;
    // 结束当前代码块。
    }
// 结束当前代码块。
}
