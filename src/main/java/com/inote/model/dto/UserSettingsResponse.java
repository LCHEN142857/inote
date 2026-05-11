package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 返回给前端的用户偏好设置快照。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    // 标记是否只允许基于引用内容回答。
    private boolean answerFromReferencesOnly;
}
