// 声明当前源文件所属包。
package com.inote.repository;

import com.inote.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 定义文档仓储接口，负责文档实体的持久化查询。
public interface DocumentRepository extends JpaRepository<Document, String> {

    // 调用 `findAllByOwnerIdOrderByUpdatedAtDesc` 完成当前步骤。
    List<Document> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    // 调用 `findByIdAndOwnerId` 完成当前步骤。
    Optional<Document> findByIdAndOwnerId(String id, String ownerId);
}
