// 声明当前源文件的包。
package com.inote.security;

import com.inote.model.entity.User;

// 处理当前代码结构。
public final class CurrentUserHolder {

    /**
     * 描述 `ThreadLocal<>` 操作。
     *
     * @return 类型为 `ThreadLocal<User> CURRENT_USER = new` 的返回值。
     */
    // 执行当前语句。
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /**
     * 描述 `CurrentUserHolder` 操作。
     *
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    private CurrentUserHolder() {
    // 结束当前代码块。
    }

    /**
     * 描述 `set` 操作。
     *
     * @param user 输入参数 `user`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public static void set(User user) {
        // 执行当前语句。
        CURRENT_USER.set(user);
    // 结束当前代码块。
    }

    /**
     * 描述 `getRequired` 操作。
     *
     * @return 类型为 `User` 的返回值。
     */
    // 处理当前代码结构。
    public static User getRequired() {
        // 执行当前语句。
        User user = CURRENT_USER.get();
        // 执行当前流程控制分支。
        if (user == null) {
            // 抛出当前异常。
            throw new UnauthorizedException("Authentication required");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return user;
    // 结束当前代码块。
    }

    /**
     * 描述 `clear` 操作。
     *
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public static void clear() {
        // 执行当前语句。
        CURRENT_USER.remove();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
