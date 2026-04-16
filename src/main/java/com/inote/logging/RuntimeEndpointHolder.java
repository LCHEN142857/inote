package com.inote.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class RuntimeEndpointHolder {

    private static volatile String host = resolveHostAddress();
    private static volatile int port = 0;

    private RuntimeEndpointHolder() {
    }

    public static void setPort(int runtimePort) {
        port = runtimePort;
    }

    public static String getEndpoint() {
        return host + ":" + port;
    }

    private static String resolveHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }
}
