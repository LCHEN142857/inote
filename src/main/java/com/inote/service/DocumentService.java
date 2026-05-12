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
            "pdf", "docx", "xlsx", "xls", "txt", "csv"
    );

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService processingService;
    private final CurrentUserService currentUserService;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        validateFile(file);
        User user = currentUserService.getCurrentUser();

        try {
            Path savedPath = processingService.saveFile(file, uploadPath);

            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(savedPath.toString())
                    .fileUrl("")
                    .owner(user)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("PARSING")
                    .active(Boolean.TRUE)
                    .build();

            document = documentRepository.save(document);
            deactivateSameNameVersions(user.getId(), document.getFileName(), document.getId());

            String fileUrl = "/api/v1/documents/files/" + document.getId();
            document.setFileUrl(fileUrl);
            document.setActive(Boolean.TRUE);
            document = documentRepository.save(document);

            processingService.processDocumentAsync(document.getId(), fileUrl);

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(document.getStatus())
                    .message("Upload accepted. Document parsing has started.")
                    .build();
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new DocumentStorageException("File save failed.", e);
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

    private void deactivateSameNameVersions(String ownerId, String fileName, String currentDocumentId) {
        List<Document> sameNameVersions = documentRepository.findAllByOwnerIdAndFileNameOrderByUpdatedAtDesc(ownerId, fileName);
        List<Document> versionsToDeactivate = sameNameVersions.stream()
                .filter(document -> !currentDocumentId.equals(document.getId()))
                .filter(this::isActiveVersion)
                .peek(document -> document.setActive(Boolean.FALSE))
                .toList();

        if (!versionsToDeactivate.isEmpty()) {
            documentRepository.saveAll(versionsToDeactivate);
        }
    }

    private boolean isActiveVersion(Document document) {
        return !Boolean.FALSE.equals(document.getActive());
    }

    private DocumentStatusResponse toStatusResponse(Document document) {
        return DocumentStatusResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .active(isActiveVersion(document))
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
            throw new IllegalArgumentException("Unsupported file type: " + extension
                    + ". Supported types: " + String.join(", ", ALLOWED_EXTENSIONS));
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
