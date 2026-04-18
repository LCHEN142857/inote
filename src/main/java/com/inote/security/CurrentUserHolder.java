// 声明当前源文件所属包。
package com.inote.security;

import com.inote.model.entity.User;

// 定义当前用户上下文容器，负责在线程内保存认证用户。
public final class CurrentUserHolder {

    // 创建当前用户对象。
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /**
     * 处理当前用户holder相关逻辑。
     */
    private CurrentUserHolder() {
    }

    /**
     * 处理set相关逻辑。
     * @param user 用户参数。
     */
    public static void set(User user) {
        // 调用 `set` 完成当前步骤。
        CURRENT_USER.set(user);
    }

    /**
     * 处理getrequired相关逻辑。
     * @return 用户结果。
     */
    public static User getRequired() {
        // 计算并保存用户结果。
        User user = CURRENT_USER.get();
        // 根据条件判断当前分支是否执行。
        if (user == null) {
            // 抛出 `UnauthorizedException` 异常中断当前流程。
            throw new UnauthorizedException("Authentication required");
        }
        // 返回用户。
        return user;
    }

    /**
     * 处理clear相关逻辑。
     */
    public static void clear() {
        // 调用 `remove` 完成当前步骤。
        CURRENT_USER.remove();
    }
}
