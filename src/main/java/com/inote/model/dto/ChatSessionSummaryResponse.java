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
public class ChatSessionSummaryResponse {

    private String id;
    private String title;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
