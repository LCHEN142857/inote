// 声明当前源文件的包。
package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

// 声明当前类型。
public class PackageNameConverter extends ClassicConverter {

    /**
     * 描述 `convert` 操作。
     *
     * @param event 输入参数 `event`。
     * @return 类型为 `String` 的返回值。
     */
    // 应用当前注解。
    @Override
    // 处理当前代码结构。
    public String convert(ILoggingEvent event) {
        // 执行当前语句。
        StackTraceElement[] callerData = event.getCallerData();
        // 执行当前流程控制分支。
        if (callerData == null || callerData.length == 0) {
            // 返回当前结果。
            return "N/A";
        // 结束当前代码块。
        }

        // 执行当前语句。
        String className = callerData[0].getClassName();
        // 执行当前语句。
        int lastDotIndex = className.lastIndexOf('.');
        // 执行当前流程控制分支。
        if (lastDotIndex <= 0) {
            // 返回当前结果。
            return className;
        // 结束当前代码块。
        }
        // 返回当前结果。
        return className.substring(0, lastDotIndex);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
