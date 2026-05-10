package com.inote.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {

    @NotNull(message = "answerFromReferencesOnly must not be null")
    private Boolean answerFromReferencesOnly;
}
