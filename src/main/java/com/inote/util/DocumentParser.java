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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 将不同格式的文档解析为纯文本，并按检索友好的方式切块。
@Slf4j
@Component
public class DocumentParser {

    /**
     * 根据文件扩展名选择对应解析器。
     * @param filePath 文件路径。
     * @param originalFilename 原始文件名。
     * @return 解析后的纯文本。
     * @throws IOException 读取文件失败时抛出。
     * @throws IllegalArgumentException 文件类型不支持或文件名缺失时抛出。
     */
    public String parse(Path filePath, String originalFilename) throws IOException {
        // 原始文件名是判断文件类型的依据。
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name must not be empty.");
        }

        // 通过扩展名分发到对应解析器。
        String extension = getFileExtension(originalFilename).toLowerCase();
        return switch (extension) {
            case "pdf" -> parsePdf(filePath, originalFilename);
            case "docx" -> parseWord(filePath, originalFilename);
            case "xlsx", "xls" -> parseExcel(filePath, originalFilename);
            case "txt", "csv" -> parseText(filePath, originalFilename);
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    /**
     * 解析 PDF 文档文本。
     * @param filePath 文件路径。
     * @param originalFilename 原始文件名。
     * @return PDF 文本内容。
     * @throws IOException 读取或解析失败时抛出。
     */
    private String parsePdf(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing PDF file: {}", originalFilename);
        // 先读取二进制内容，再交给 PDFBox 解析。
        byte[] pdfBytes = Files.readAllBytes(filePath);
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 Word 文档文本。
     * @param filePath 文件路径。
     * @param originalFilename 原始文件名。
     * @return Word 段落拼接后的文本。
     * @throws IOException 读取或解析失败时抛出。
     */
    private String parseWord(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing Word file: {}", originalFilename);
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(is)) {
            // 直接拼接段落文本，保留原始阅读顺序。
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析 Excel 文档文本。
     * @param filePath 文件路径。
     * @param originalFilename 原始文件名。
     * @return 每个工作表和单元格内容的文本表示。
     * @throws IOException 读取或解析失败时抛出。
     */
    private String parseExcel(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing Excel file: {}", originalFilename);
        // 用 StringBuilder 累积工作表文本。
        StringBuilder content = new StringBuilder();

        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = createWorkbook(is, originalFilename)) {
            // 遍历每个工作表并保留名称。
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                // 逐行提取非空单元格。
                for (Row row : sheet) {
                    StringBuilder rowContent = new StringBuilder();
                    for (Cell cell : row) {
                        // 统一转换不同单元格类型的值。
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
     * 根据扩展名创建对应的 Workbook。
     * @param is 文件输入流。
     * @param filename 原始文件名。
     * @return Excel 工作簿。
     * @throws IOException 创建工作簿失败时抛出。
     */
    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        // xlsx 使用 OOXML 工作簿，其他 Excel 格式使用 HSSF。
        String extension = getFileExtension(filename).toLowerCase();
        if ("xlsx".equals(extension)) {
            return new XSSFWorkbook(is);
        }
        return new HSSFWorkbook(is);
    }

    /**
     * 将单元格值转换为字符串。
     * @param cell 当前单元格。
     * @return 单元格文本值。
     */
    private String getCellValueAsString(Cell cell) {
        // 空单元格直接返回空串。
        if (cell == null) {
            return "";
        }

        // 根据单元格类型提取最适合检索的文本。
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * 解析纯文本或 CSV 文件。
     * @param filePath 文件路径。
     * @param originalFilename 原始文件名。
     * @return 文件文本内容。
     * @throws IOException 读取失败时抛出。
     */
    private String parseText(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing text file: {}", originalFilename);
        try (InputStream is = Files.newInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // 直接按行拼接，保留文本顺序。
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 提取文件扩展名。
     * @param filename 原始文件名。
     * @return 扩展名，缺失时返回空串。
     */
    private String getFileExtension(String filename) {
        // 没有点号或点号在末尾时视为无扩展名。
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 将长文本按段落切块，保留一定重叠以便检索召回。
     * @param text 待切块文本。
     * @param chunkSize 单块目标长度。
     * @param overlap 相邻切块重叠长度。
     * @return 文本块列表。
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        // 空文本直接返回空列表。
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 以空行分隔段落，减少语义被硬切断的概率。
        String[] paragraphs = text.split("\n\\s*\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        // 逐段累积直到接近目标长度。
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // 当前块过长时先落盘，再保留尾部重叠内容。
            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // 保留尾部片段作为下一块上下文。
                String currentText = currentChunk.toString();
                if (currentText.length() > overlap) {
                    currentChunk = new StringBuilder(currentText.substring(currentText.length() - overlap));
                } else {
                    currentChunk = new StringBuilder();
                }
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // 收尾时补上最后一个未提交的块。
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
