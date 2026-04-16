// 声明当前源文件的包。
package com.inote.repository;

import com.inote.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 声明当前类型。
public interface DocumentRepository extends JpaRepository<Document, String> {

    /**
     * 描述 `findAllByOwnerIdOrderByUpdatedAtDesc` 操作。
     *
     * @param ownerId 输入参数 `ownerId`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 执行当前语句。
    List<Document> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    /**
     * 描述 `findByIdAndOwnerId` 操作。
     *
     * @param id 输入参数 `id`。
     * @param ownerId 输入参数 `ownerId`。
     * @return 类型为 `Optional<Document>` 的返回值。
     */
    // 执行当前语句。
    Optional<Document> findByIdAndOwnerId(String id, String ownerId);
// 结束当前代码块。
}
