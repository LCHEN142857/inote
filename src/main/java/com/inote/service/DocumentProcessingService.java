package com.inote.service;

import com.inote.model.entity.Document;
import com.inote.repository.DocumentRepository;
import com.inote.service.retrieval.BM25SearchService;
import com.inote.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private final DocumentParser documentParser;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final BM25SearchService bm25SearchService;

    @Async
    public void processDocumentAsync(Document document, MultipartFile file, String fileUrl) {
        try {
            log.info("Starting async processing for document: {}", document.getId());

            document.setStatus("PARSING");
            documentRepository.save(document);

            String content = documentParser.parse(file);
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            log.info("Document {} split into {} chunks", document.getId(), chunks.size());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", document.getId());
            metadata.put("file_name", document.getFileName());
            metadata.put("file_url", fileUrl);
            metadata.put("content_type", document.getContentType());

            embeddingService.embedAndStore(chunks, metadata);

            try {
                List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                        .map(chunk -> {
                            Map<String, Object> docMeta = new HashMap<>(metadata);
                            docMeta.put("chunk_size", chunk.length());
                            return new org.springframework.ai.document.Document(chunk, docMeta);
                        })
                        .toList();
                bm25SearchService.indexDocuments(aiDocuments);
            } catch (Exception e) {
                log.warn("BM25 indexing failed for document {}: {}", document.getId(), e.getMessage());
            }

            document.setStatus("COMPLETED");
            document.setErrorMessage(null);
            documentRepository.save(document);
            log.info("Document processing completed: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getId(), e);
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }

    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFileName = fileExtension.isEmpty()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + fileExtension;

        Path targetPath = Paths.get(uploadPath, newFileName);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath);
        return targetPath;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
