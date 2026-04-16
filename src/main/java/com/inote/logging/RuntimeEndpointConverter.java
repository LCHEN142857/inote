package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class RuntimeEndpointConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return RuntimeEndpointHolder.getEndpoint();
    }
}
