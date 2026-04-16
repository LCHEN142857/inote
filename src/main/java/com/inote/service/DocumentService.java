package com.inote.service;

import com.inote.model.dto.DocumentStatusResponse;
import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;
import com.inote.repository.DocumentRepository;
import com.inote.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "txt", "csv"
    );

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService processingService;
    private final CurrentUserService currentUserService;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        try {
            validateFile(file);
            User user = currentUserService.getCurrentUser();

            Path savedPath = processingService.saveFile(file, uploadPath);

            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(savedPath.toString())
                    .fileUrl("")
                    .owner(user)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("PARSING")
                    .build();

            document = documentRepository.save(document);
            String fileUrl = "/api/v1/documents/files/" + document.getId();
            document.setFileUrl(fileUrl);
            documentRepository.save(document);
            processingService.processDocumentAsync(document, file, fileUrl);

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(document.getStatus())
                    .message("Upload accepted. Document parsing has started.")
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("File validation failed: {}", e.getMessage());
            return DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        } catch (IOException e) {
            log.error("Failed to save file", e);
            return DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message("File save failed: " + e.getMessage())
                    .build();
        }
    }

    public DocumentStatusResponse getDocumentStatus(String documentId) {
        return toStatusResponse(findDocument(documentId));
    }

    public List<DocumentStatusResponse> listDocuments() {
        User user = currentUserService.getCurrentUser();
        return documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(this::toStatusResponse)
                .toList();
    }

    public Document getOwnedDocument(String documentId) {
        return findDocument(documentId);
    }

    private Document findDocument(String documentId) {
        User user = currentUserService.getCurrentUser();
        return documentRepository.findByIdAndOwnerId(documentId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    private DocumentStatusResponse toStatusResponse(Document document) {
        return DocumentStatusResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name must not be empty.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension +
                    ". Supported types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
