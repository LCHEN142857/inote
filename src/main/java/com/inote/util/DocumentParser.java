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

@Slf4j
@Component
public class DocumentParser {

    public String parse(Path filePath, String originalFilename) throws IOException {
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name must not be empty.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        return switch (extension) {
            case "pdf" -> parsePdf(filePath, originalFilename);
            case "docx" -> parseWord(filePath, originalFilename);
            case "xlsx", "xls" -> parseExcel(filePath, originalFilename);
            case "txt", "csv" -> parseText(filePath, originalFilename);
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    private String parsePdf(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing PDF file: {}", originalFilename);
        byte[] pdfBytes = Files.readAllBytes(filePath);
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseWord(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing Word file: {}", originalFilename);
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(is)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String parseExcel(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing Excel file: {}", originalFilename);
        StringBuilder content = new StringBuilder();

        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = createWorkbook(is, originalFilename)) {
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

    private Workbook createWorkbook(InputStream is, String filename) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();
        if ("xlsx".equals(extension)) {
            return new XSSFWorkbook(is);
        }
        return new HSSFWorkbook(is);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

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

    private String parseText(Path filePath, String originalFilename) throws IOException {
        log.debug("Parsing text file: {}", originalFilename);
        try (InputStream is = Files.newInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        String[] paragraphs = text.split("\n\\s*\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                String currentText = currentChunk.toString();
                if (currentText.length() > overlap) {
                    currentChunk = new StringBuilder(currentText.substring(currentText.length() - overlap));
                } else {
                    currentChunk = new StringBuilder();
                }
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
