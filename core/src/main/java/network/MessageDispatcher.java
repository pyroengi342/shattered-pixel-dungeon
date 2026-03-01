package network;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.handlers.MessageHandler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {

    private final Map<String, MessageHandler> handlers = new HashMap<>();

    public void registerHandler(MessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void dispatch(NetworkManager.BundleMessage message) {
        Game.runOnRenderThread(() -> {
            MessageHandler handler = handlers.get(message.type);
            if (handler == null) {
                System.err.println("No handler for message type: " + message.type);
                return;
            }

            Bundle bundle = null;
            if (message.bundleData != null && !message.bundleData.isEmpty()) {
                try {
                    bundle = Bundle.read(new ByteArrayInputStream(
                            message.bundleData.getBytes(StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            handler.msgHandle(message.playerId, bundle);
        });
    }
}