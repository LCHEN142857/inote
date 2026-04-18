// 声明当前源文件所属包。
package com.inote.security;

import com.inote.model.entity.User;
import org.springframework.stereotype.Service;

// 将当前类注册为服务组件。
@Service
// 定义当前用户服务，负责读取已认证用户信息。
public class CurrentUserService {

    /**
     * 处理get当前用户相关逻辑。
     * @return 用户结果。
     */
    public User getCurrentUser() {
        // 返回 `getRequired` 的处理结果。
        return CurrentUserHolder.getRequired();
    }
}
