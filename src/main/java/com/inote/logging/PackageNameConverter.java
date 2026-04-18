// 声明当前源文件所属包。
package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

// 定义日志包名转换器，用于压缩日志中的包路径。
public class PackageNameConverter extends ClassicConverter {

    /**
     * 处理convert相关逻辑。
     * @param event event参数。
     * @return 处理后的字符串结果。
     */
    // 声明当前方法重写父类或接口定义。
    @Override
    public String convert(ILoggingEvent event) {
        // 计算并保存callerdata结果。
        StackTraceElement[] callerData = event.getCallerData();
        // 根据条件判断当前分支是否执行。
        if (callerData == null || callerData.length == 0) {
            // 返回"n/a"。
            return "N/A";
        }

        // 计算并保存classname结果。
        String className = callerData[0].getClassName();
        // 计算并保存lastdotindex结果。
        int lastDotIndex = className.lastIndexOf('.');
        // 根据条件判断当前分支是否执行。
        if (lastDotIndex <= 0) {
            // 返回classname。
            return className;
        }
        // 返回 `substring` 的处理结果。
        return className.substring(0, lastDotIndex);
    }
}
