package com.inote.service;

import com.inote.model.dto.UserSettingsRequest;
import com.inote.model.dto.UserSettingsResponse;
import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import com.inote.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 管理当前登录用户的回答偏好设置。
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    // 提供当前认证用户上下文。
    private final CurrentUserService currentUserService;
    // 持久化用户偏好字段。
    private final UserRepository userRepository;

    /**
     * 查询当前用户的回答偏好。
     * @return 当前用户设置响应。
     * @throws com.inote.security.UnauthorizedException 当前请求未认证时抛出。
     */
    @Transactional(readOnly = true)
    public UserSettingsResponse currentSettings() {
        // 从安全上下文中取出当前用户实体。
        User user = currentUserService.getCurrentUser();
        // 将用户实体转换成前端所需的设置响应。
        return toResponse(user);
    }

    /**
     * 更新当前用户的回答偏好。
     * @param request 设置更新请求。
     * @return 保存后的用户设置响应。
     * @throws com.inote.security.UnauthorizedException 当前请求未认证时抛出。
     */
    @Transactional
    public UserSettingsResponse updateSettings(UserSettingsRequest request) {
        // 设置只允许修改当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 应用前端提交的引用来源限制偏好。
        user.setAnswerFromReferencesOnly(request.getAnswerFromReferencesOnly());
        // 保存后返回持久化结果，避免响应旧状态。
        return toResponse(userRepository.save(user));
    }

    /**
     * 判断回答是否应限制在引用资料内。
     * @return true 表示仅基于引用回答，false 表示允许模型自由回答。
     * @throws com.inote.security.UnauthorizedException 当前请求未认证时抛出。
     */
    public boolean answerFromReferencesOnly() {
        // 读取当前用户偏好，空值按默认开启处理。
        User user = currentUserService.getCurrentUser();
        return Boolean.TRUE.equals(user.getAnswerFromReferencesOnly()) || user.getAnswerFromReferencesOnly() == null;
    }

    /**
     * 构造用户设置响应。
     * @param user 当前登录用户。
     * @return 用户设置响应对象。
     */
    private UserSettingsResponse toResponse(User user) {
        // 空偏好代表历史用户默认启用仅引用回答。
        return UserSettingsResponse.builder()
                .answerFromReferencesOnly(Boolean.TRUE.equals(user.getAnswerFromReferencesOnly()) || user.getAnswerFromReferencesOnly() == null)
                .build();
    }
}
