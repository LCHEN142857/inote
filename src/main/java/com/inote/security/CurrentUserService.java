// 声明当前源文件的包。
package com.inote.security;

import com.inote.model.entity.User;
import org.springframework.stereotype.Service;

// 应用当前注解。
@Service
// 声明当前类型。
public class CurrentUserService {

    /**
     * 描述 `getCurrentUser` 操作。
     *
     * @return 类型为 `User` 的返回值。
     */
    // 处理当前代码结构。
    public User getCurrentUser() {
        // 返回当前结果。
        return CurrentUserHolder.getRequired();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
