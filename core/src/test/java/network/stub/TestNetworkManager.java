/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package network.stub;

import network.handlers.MessageHandler;
import com.watabou.utils.Bundle;

import java.util.*;

/**
 * Test stub for NetworkManager that doesn't require LWJGL.
 * Allows testing message flow and handler logic.
 */
public class TestNetworkManager {

    public enum Mode {
        NONE, SERVER, CLIENT, LOCALHOST
    }
    
    private Mode mode = Mode.NONE;
    private final Map<String, List<MessageHandler>> handlers = new HashMap<>();
    private final List<ReceivedMessage> receivedMessages = new ArrayList<>();
    private boolean multiplayer = false;
    private boolean isHost = false;

    public void startServer() {
        mode = Mode.SERVER;
        multiplayer = true;
        isHost = true;
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = true;
    }

    public void connectToServer(String host) {
        mode = Mode.CLIENT;
        multiplayer = true;
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = false;
    }

    public void disconnect() {
        mode = Mode.NONE;
        multiplayer = false;
        isHost = false;
        network.Multiplayer.isMultiplayer = false;
        network.Multiplayer.isHost = false;
    }

    public Mode getMode() {
        return mode;
    }

    public void sendMessage(String type, Bundle bundle) {
        receivedMessages.add(new ReceivedMessage(type, -1, bundle));
    }

    public void simulateMessage(String type, int senderId, Bundle bundle) {
        receivedMessages.add(new ReceivedMessage(type, senderId, bundle));
        List<MessageHandler> typeHandlers = handlers.get(type);
        if (typeHandlers != null) {
            for (MessageHandler h : typeHandlers) {
                h.msgHandle(senderId, bundle);
            }
        }
    }

    public void registerTestHandler(MessageHandler handler) {
        handlers.computeIfAbsent(handler.getType(), k -> new ArrayList<>()).add(handler);
    }

    public List<ReceivedMessage> getReceivedMessages() {
        return receivedMessages;
    }

    public void reset() {
        mode = Mode.NONE;
        multiplayer = false;
        isHost = false;
        receivedMessages.clear();
        handlers.clear();
    }

    public static class ReceivedMessage {
        public String type;
        public int senderId;
        public Bundle bundle;
        
        public ReceivedMessage(String type, int senderId, Bundle bundle) {
            this.type = type;
            this.senderId = senderId;
            this.bundle = bundle;
        }
    }
}
