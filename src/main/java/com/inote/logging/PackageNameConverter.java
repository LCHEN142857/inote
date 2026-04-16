package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PackageNameConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        StackTraceElement[] callerData = event.getCallerData();
        if (callerData == null || callerData.length == 0) {
            return "N/A";
        }

        String className = callerData[0].getClassName();
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return className;
        }
        return className.substring(0, lastDotIndex);
    }
}
