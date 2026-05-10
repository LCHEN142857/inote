package com.inote.service;

import com.inote.model.dto.UserSettingsRequest;
import com.inote.model.dto.UserSettingsResponse;
import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import com.inote.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingsResponse currentSettings() {
        User user = currentUserService.getCurrentUser();
        return toResponse(user);
    }

    @Transactional
    public UserSettingsResponse updateSettings(UserSettingsRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setAnswerFromReferencesOnly(request.getAnswerFromReferencesOnly());
        return toResponse(userRepository.save(user));
    }

    public boolean answerFromReferencesOnly() {
        User user = currentUserService.getCurrentUser();
        return Boolean.TRUE.equals(user.getAnswerFromReferencesOnly()) || user.getAnswerFromReferencesOnly() == null;
    }

    private UserSettingsResponse toResponse(User user) {
        return UserSettingsResponse.builder()
                .answerFromReferencesOnly(Boolean.TRUE.equals(user.getAnswerFromReferencesOnly()) || user.getAnswerFromReferencesOnly() == null)
                .build();
    }
}
