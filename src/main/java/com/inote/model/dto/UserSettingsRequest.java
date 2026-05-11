package com.inote.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 代表用户的偏好设置更新请求。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {

    // 控制回答是否仅基于引用内容生成。
    @NotNull(message = "answerFromReferencesOnly must not be null")
    private Boolean answerFromReferencesOnly;
}
