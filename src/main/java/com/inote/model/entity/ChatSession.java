// 声明当前源文件的包。
package com.inote.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 应用当前注解。
@Getter
// 应用当前注解。
@Setter
// 应用当前注解。
@Builder
// 应用当前注解。
@NoArgsConstructor
// 应用当前注解。
@AllArgsConstructor
// 应用当前注解。
@Entity
// 应用当前注解。
@Table(name = "chat_sessions")
// 应用当前注解。
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// 应用当前注解。
@ToString(exclude = "messages")
// 声明当前类型。
public class ChatSession {

    // 应用当前注解。
    @Id
    // 应用当前注解。
    @EqualsAndHashCode.Include
    // 应用当前注解。
    @Column(length = 36, nullable = false, updatable = false)
    // 声明当前字段。
    private String id;

    // 应用当前注解。
    @Column(nullable = false, length = 255)
    // 声明当前字段。
    private String title;

    // 应用当前注解。
    @ManyToOne(optional = false)
    // 应用当前注解。
    @JoinColumn(name = "owner_id", nullable = false)
    // 声明当前字段。
    private User owner;

    /**
     * 描述 `ArrayList<>` 操作。
     *
     * @return 类型为 `List<ChatMessage> messages = new` 的返回值。
     */
    // 应用当前注解。
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    // 应用当前注解。
    @Builder.Default
    // 执行当前语句。
    private List<ChatMessage> messages = new ArrayList<>();

    // 应用当前注解。
    @Column(nullable = false)
    // 声明当前字段。
    private LocalDateTime createdAt;

    // 应用当前注解。
    @Column(nullable = false)
    // 声明当前字段。
    private LocalDateTime updatedAt;

    /**
     * 描述 `prePersist` 操作。
     *
     * @return 无返回值。
     */
    // 应用当前注解。
    @PrePersist
    // 处理当前代码结构。
    void prePersist() {
        // 执行当前流程控制分支。
        if (id == null) {
            // 执行当前语句。
            id = UUID.randomUUID().toString();
        // 结束当前代码块。
        }
        // 执行当前语句。
        LocalDateTime now = LocalDateTime.now();
        // 执行当前语句。
        createdAt = now;
        // 执行当前语句。
        updatedAt = now;
    // 结束当前代码块。
    }

    /**
     * 描述 `preUpdate` 操作。
     *
     * @return 无返回值。
     */
    // 应用当前注解。
    @PreUpdate
    // 处理当前代码结构。
    void preUpdate() {
        // 执行当前语句。
        updatedAt = LocalDateTime.now();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
