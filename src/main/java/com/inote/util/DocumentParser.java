package com.inote.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档解析工具类
 * 支持 PDF、Word、Excel、TXT、CSV 格式
 */
@Slf4j
@Component
public class DocumentParser {

    /**
     * 解析文档内容
     * @param file 上传的文件
     * @return 文档文本内容
     */
    public String parse(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        
        switch (extension) {
            case "pdf":
                return parsePdf(file);
            case "docx":
            case "doc":
                return parseWord(file);
            case "xlsx":
            case "xls":
                return parseExcel(file);
            case "txt":
            case "csv":
                return parseText(file);
            default:
                throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }
    }

    /**
     * 解析 PDF 文件
     */
    private String parsePdf(MultipartFile file) throws IOException {
        log.debug("Parsing PDF file: {}", file.getOriginalFilename());
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 Word 文件
     */
    private String parseWord(MultipartFile file) throws IOException {
        log.debug("Parsing Word file: {}", file.getOriginalFilename());
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析 Excel 文件
     */
    private String parseExcel(MultipartFile file) throws IOException {
        log.debug("Parsing Excel file: {}", file.getOriginalFilename());
        StringBuilder content = new StringBuilder();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = createWorkbook(is, file.getOriginalFilename())) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (Row row : sheet) {
                    StringBuilder rowContent = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (!cellValue.isEmpty()) {
                            rowContent.append(cellValue).append("\t");
                        }
                    }
                    if (rowContent.length() > 0) {
                        content.append(rowContent.toString().trim()).append("\n");
                    }
                }
                content.append("\n");
            }
        }
        
        return content.toString().trim();
    }

    /**
     * 创建 Workbook 对象
     */
    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();
        if ("xlsx".equals(extension)) {
            return new XSSFWorkbook(is);
        } else {
            return new HSSFWorkbook(is);
        }
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 解析文本文件 (TXT, CSV)
     */
    private String parseText(MultipartFile file) throws IOException {
        log.debug("Parsing text file: {}", file.getOriginalFilename());
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 将文本分割成块
     * @param text 原始文本
     * @param chunkSize 块大小（字符数）
     * @param overlap 重叠大小
     * @return 文本块列表
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 按段落分割
        String[] paragraphs = text.split("\n\s*\n");
        
        java.util.List<String> chunks = new java.util.ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // 如果当前段落加上已有内容超过块大小，先保存当前块
            if (currentChunk.length() + paragraph.length() > chunkSize 
                    && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // 保留重叠部分
                String chunkText = currentChunk.toString();
                if (chunkText.length() > overlap) {
                    currentChunk = new StringBuilder(
                            chunkText.substring(chunkText.length() - overlap));
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            
            currentChunk.append(paragraph).append("\n\n");
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}
