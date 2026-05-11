package com.inote.service;

import com.inote.config.ChatModelProperties;
import com.inote.model.dto.ChatModelCatalogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatModelSelectionService {

    private final ChatModelProperties chatModelProperties;

    public String resolveModel(String requestedModel) {
        String defaultModel = defaultModel();
        if (!StringUtils.hasText(requestedModel)) {
            return defaultModel;
        }
        if (chatModelProperties.getAvailableModels().contains(requestedModel)) {
            return requestedModel;
        }
        log.warn("Unsupported chat model '{}', falling back to '{}'", requestedModel, defaultModel);
        return defaultModel;
    }

    public OpenAiChatOptions buildOptions(String requestedModel) {
        return OpenAiChatOptions.builder()
                .model(resolveModel(requestedModel))
                .build();
    }

    public ChatModelCatalogResponse catalog() {
        return ChatModelCatalogResponse.builder()
                .defaultModel(defaultModel())
                .availableModels(List.copyOf(chatModelProperties.getAvailableModels()))
                .build();
    }

    private String defaultModel() {
        if (StringUtils.hasText(chatModelProperties.getDefaultModel())) {
            return chatModelProperties.getDefaultModel();
        }
        return chatModelProperties.getAvailableModels().isEmpty()
                ? ""
                : chatModelProperties.getAvailableModels().get(0);
    }
}
