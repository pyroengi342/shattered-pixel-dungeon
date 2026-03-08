package network.utils;

public class ErrorMessageUtil{
    private ErrorMessageUtil() {
        /* This utility class should not be instantiated */
    }

    public static String getDetailedErrorMessage(Throwable cause) {
        if (cause == null) return "Unknown error";
        if (cause instanceof io.netty.channel.ConnectTimeoutException) {
            return "Connection timeout - server might be offline";
        } else if (cause instanceof java.net.ConnectException) {
            return "Cannot connect to server - check address and port";
        } else if (cause instanceof java.net.UnknownHostException) {
            return "Unknown host - check server address";
        } else if (cause instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return "Server timeout - no response received";
        } else if (cause instanceof java.io.IOException) {
            return "Network error: " + cause.getMessage();
        } else if (cause instanceof com.esotericsoftware.kryo.KryoException) {
            return "Data serialization error";
        } else if (cause instanceof io.netty.handler.codec.DecoderException) {
            return "Protocol error - invalid data received";
        }
        return cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
    }
}
