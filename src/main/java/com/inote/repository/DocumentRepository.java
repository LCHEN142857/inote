package com.inote.repository;

import com.inote.model.entity.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档仓库 - 内存实现
 * 生产环境建议替换为数据库实现
 */
@Slf4j
@Repository
public class DocumentRepository {

    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    public Document save(Document document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setUpdatedAt(LocalDateTime.now());
        
        documentStore.put(document.getId(), document);
        log.debug("Document saved: {}", document.getId());
        return document;
    }

    public Optional<Document> findById(String id) {
        return Optional.ofNullable(documentStore.get(id));
    }

    public void deleteById(String id) {
        documentStore.remove(id);
        log.debug("Document deleted: {}", id);
    }

    public Map<String, Document> findAll() {
        return new ConcurrentHashMap<>(documentStore);
    }
}
