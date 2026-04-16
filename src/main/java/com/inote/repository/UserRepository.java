// 声明当前源文件的包。
package com.inote.repository;

import com.inote.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 声明当前类型。
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 描述 `findByUsername` 操作。
     *
     * @param username 输入参数 `username`。
     * @return 类型为 `Optional<User>` 的返回值。
     */
    // 执行当前语句。
    Optional<User> findByUsername(String username);

    /**
     * 描述 `findByAuthToken` 操作。
     *
     * @param authToken 输入参数 `authToken`。
     * @return 类型为 `Optional<User>` 的返回值。
     */
    // 执行当前语句。
    Optional<User> findByAuthToken(String authToken);
// 结束当前代码块。
}
