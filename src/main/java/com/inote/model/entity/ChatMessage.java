// 声明当前源文件所属包。
package com.inote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
@Table(name = "chat_messages")
// 应用 `EqualsAndHashCode` 注解声明当前行为。
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// 应用 `ToString` 注解声明当前行为。
@ToString(exclude = "session")
// 定义消息实体，表示会话中的一条对话消息。
public class ChatMessage {

    // 标记当前字段作为实体主键。
    @Id
    // 应用 `EqualsAndHashCode` 注解声明当前行为。
    @EqualsAndHashCode.Include
    // 定义当前属性与数据库列的映射关系。
    @Column(length = 36, nullable = false, updatable = false)
    // 声明id变量，供后续流程使用。
    private String id;

    // 定义当前实体到上游实体的多对一关系。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // 声明实体关联关系的外键列。
    @JoinColumn(name = "session_id", nullable = false)
    // 声明会话变量，供后续流程使用。
    private ChatSession session;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, length = 20)
    // 声明role变量，供后续流程使用。
    private String role;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, columnDefinition = "TEXT")
    // 声明内容变量，供后续流程使用。
    private String content;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false)
    // 声明createdat变量，供后续流程使用。
    private LocalDateTime createdAt;

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
        // 计算并保存createdat结果。
        createdAt = LocalDateTime.now();
    }
}
