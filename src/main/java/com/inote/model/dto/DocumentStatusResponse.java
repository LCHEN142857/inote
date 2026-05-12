package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusResponse {

    private String documentId;
    private String fileName;
    private long fileSize;
    private String status;
    private Boolean active;
    private String errorMessage;
    private LocalDateTime updatedAt;
}
