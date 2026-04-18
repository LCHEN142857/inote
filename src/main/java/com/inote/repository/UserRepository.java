// 声明当前源文件所属包。
package com.inote.repository;

import com.inote.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 定义用户仓储接口，负责用户实体的持久化查询。
public interface UserRepository extends JpaRepository<User, String> {

    // 调用 `findByUsername` 完成当前步骤。
    Optional<User> findByUsername(String username);

    // 调用 `findByAuthToken` 完成当前步骤。
    Optional<User> findByAuthToken(String authToken);
}
