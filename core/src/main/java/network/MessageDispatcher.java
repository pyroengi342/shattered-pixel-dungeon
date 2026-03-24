package network;

import com.esotericsoftware.kryo.Kryo;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.handlers.MessageHandler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {

    private final Map<String, MessageHandler> handlers = new HashMap<>();
    private Kryo kryo;
    
    public void setKryo(Kryo kryo) {
        this.kryo = kryo;
    }

    public void registerHandler(MessageHandler handler)
    { handlers.put(handler.getType(), handler); }

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
    
    /**
     * Dispatch a GameMessage with efficient Kryo deserialization.
     */
    public void dispatch(GameMessage message) {
        Game.runOnRenderThread(() -> {
            String type = message.getType();
            MessageHandler handler = handlers.get(type);
            if (handler == null) {
                System.err.println("No handler for message type: " + type);
                return;
            }

            Bundle bundle = null;
            if (message.data != null && message.data.length > 0) {
                try {
                    if (kryo != null) {
                        // Use efficient Kryo deserialization
                        bundle = GameMessage.deserializeBundle(message.data, kryo);
                    } else {
                        // Fallback to legacy method
                        bundle = Bundle.read(new ByteArrayInputStream(
                                message.data));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            handler.msgHandle(message.playerId, bundle);
        });
    }
    
    /**
     * Dispatch any message - automatically detects type.
     */
    public void dispatchAuto(Object message) {
        if (message instanceof GameMessage) {
            dispatch((GameMessage) message);
        } else if (message instanceof NetworkManager.BundleMessage) {
            dispatch((NetworkManager.BundleMessage) message);
        } else {
            System.err.println("Unknown message type: " + message.getClass());
        }
    }
}