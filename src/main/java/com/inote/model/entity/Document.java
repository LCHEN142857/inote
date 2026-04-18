// 声明当前源文件所属包。
package com.inote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// 应用 `Getter` 注解声明当前行为。
@Getter
// 应用 `Setter` 注解声明当前行为。
@Setter
// 让 Lombok 为当前类型生成建造者。
@Builder
// 让 Lombok 生成无参构造函数。
@NoArgsConstructor
// 让 Lombok 生成全参构造函数。
@AllArgsConstructor
// 声明当前类型为持久化实体。
@Entity
// 声明当前实体映射的数据表。
@Table(name = "documents")
// 应用 `EqualsAndHashCode` 注解声明当前行为。
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// 定义文档实体，表示用户上传并解析的知识库文件。
public class Document {

    // 标记当前字段作为实体主键。
    @Id
    // 应用 `EqualsAndHashCode` 注解声明当前行为。
    @EqualsAndHashCode.Include
    // 定义当前属性与数据库列的映射关系。
    @Column(length = 36, nullable = false, updatable = false)
    // 声明id变量，供后续流程使用。
    private String id;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明文件name变量，供后续流程使用。
    private String fileName;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明文件path变量，供后续流程使用。
    private String filePath;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明文件url变量，供后续流程使用。
    private String fileUrl;

    // 定义当前实体到上游实体的多对一关系。
    @ManyToOne(optional = false)
    // 声明实体关联关系的外键列。
    @JoinColumn(name = "owner_id", nullable = false)
    // 声明所属用户变量，供后续流程使用。
    private User owner;

    // 声明内容type变量，供后续流程使用。
    private String contentType;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明文件size变量，供后续流程使用。
    private long fileSize;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, length = 32)
    // 声明状态变量，供后续流程使用。
    private String status;

    // 定义当前属性与数据库列的映射关系。
    @Column(columnDefinition = "TEXT")
    // 声明错误信息消息变量，供后续流程使用。
    private String errorMessage;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明createdat变量，供后续流程使用。
    private LocalDateTime createdAt;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明updatedat变量，供后续流程使用。
    private LocalDateTime updatedAt;

    /**
     * 处理prepersist相关逻辑。
     */
    // 应用 `PrePersist` 注解声明当前行为。
    @PrePersist
    void prePersist() {
        // 根据条件判断当前分支是否执行。
        if (id == null) {
            // 生成id随机值。
            id = UUID.randomUUID().toString();
        }
        // 计算并保存now结果。
        LocalDateTime now = LocalDateTime.now();
        // 计算并保存createdat结果。
        createdAt = now;
        // 计算并保存updatedat结果。
        updatedAt = now;
    }

    /**
     * 处理preupdate相关逻辑。
     */
    // 应用 `PreUpdate` 注解声明当前行为。
    @PreUpdate
    void preUpdate() {
        // 计算并保存updatedat结果。
        updatedAt = LocalDateTime.now();
    }
}
