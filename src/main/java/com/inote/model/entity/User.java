// 声明当前源文件所属包。
package com.inote.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "users")
// 应用 `EqualsAndHashCode` 注解声明当前行为。
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// 定义用户实体，表示系统中的登录用户。
public class User {

    // 标记当前字段作为实体主键。
    @Id
    // 应用 `EqualsAndHashCode` 注解声明当前行为。
    @EqualsAndHashCode.Include
    // 定义当前属性与数据库列的映射关系。
    @Column(length = 36, nullable = false, updatable = false)
    // 声明id变量，供后续流程使用。
    private String id;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, unique = true, length = 64)
    // 声明username变量，供后续流程使用。
    private String username;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, length = 128)
    // 声明密码hash变量，供后续流程使用。
    private String passwordHash;

    // 定义当前属性与数据库列的映射关系。
    @Column(nullable = false, unique = true, length = 64)
    // 声明认证令牌变量，供后续流程使用。
    private String authToken;

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
