// 声明包路径，数据访问层
package com.inote.repository;

// 导入文档实体类
import com.inote.model.entity.Document;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 Spring 仓库注解
import org.springframework.stereotype.Repository;

// 导入日期时间类型
import java.time.LocalDateTime;
// 导入 Map 集合接口
import java.util.Map;
// 导入 Optional 容器类，用于安全处理可能为 null 的值
import java.util.Optional;
// 导入 UUID 工具类，用于生成唯一标识
import java.util.UUID;
// 导入线程安全的 HashMap 实现
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档仓库 - 基于内存的实现
 * 使用 ConcurrentHashMap 存储文档数据，生产环境建议替换为数据库实现
 */
// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 数据访问层组件，纳入 Spring 容器管理
@Repository
// 文档仓库类，提供文档的增删查操作
public class DocumentRepository {

    // 内存文档存储，使用 ConcurrentHashMap 保证多线程安全
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    // 保存或更新文档记录
    public Document save(Document document) {
        // 如果文档没有 ID，说明是新文档
        if (document.getId() == null) {
            // 生成 UUID 作为文档唯一标识
            document.setId(UUID.randomUUID().toString());
        }
        // 如果创建时间为空，说明是首次保存
        if (document.getCreatedAt() == null) {
            // 设置创建时间为当前时间
            document.setCreatedAt(LocalDateTime.now());
        }
        // 每次保存都更新最后修改时间
        document.setUpdatedAt(LocalDateTime.now());

        // 将文档存入内存 Map 中
        documentStore.put(document.getId(), document);
        // 记录保存成功的日志
        log.debug("Document saved: {}", document.getId());
        // 返回保存后的文档（包含生成的 ID 和时间戳）
        return document;
    }

    // 根据 ID 查找文档
    public Optional<Document> findById(String id) {
        // 从 Map 中获取文档，用 Optional 包装避免 null
        return Optional.ofNullable(documentStore.get(id));
    }

    // 根据 ID 删除文档
    public void deleteById(String id) {
        // 从 Map 中移除指定文档
        documentStore.remove(id);
        // 记录删除成功的日志
        log.debug("Document deleted: {}", id);
    }

    // 查询所有文档
    public Map<String, Document> findAll() {
        // 返回文档 Map 的副本，避免外部直接修改原始数据
        return new ConcurrentHashMap<>(documentStore);
    }
}
