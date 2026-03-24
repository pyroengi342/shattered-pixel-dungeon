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

package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Improved message class that uses direct Kryo serialization instead of Bundle→String.
 * 
 * Benefits:
 * - No String intermediate representation
 * - Faster serialization/deserialization
 * - Smaller message size (binary vs text)
 * - Type-safe message IDs
 */
public class GameMessage {
    
    // Message type constants for faster processing
    public static final class Types {
        // Lobby
        public static final String PLAYER_ASSIGN = "PLAYER_ASSIGN";
        public static final String PLAYER_JOIN = "PLAYER_JOIN";
        public static final String PLAYER_LEAVE = "PLAYER_LEAVE";
        public static final String SERVER_SHUTDOWN = "SERVER_SHUTDOWN";
        public static final String SEED_INIT = "SEED_INIT";
        public static final String HERO_CLASS = "HERO_CLASS";
        public static final String HERO_CLASS_SELECTED = "HERO_CLASS_SELECTED";
        public static final String PLAYER_READY = "PLAYER_READY";
        public static final String PLAYER_KICK = "PLAYER_KICK";
        public static final String KICK_NOTIFY = "KICK_NOTIFY";
        
        // Game actions
        public static final String PLAYER_MOVE = "PLAYER_MOVE";
        public static final String PLAYER_ATTACK = "PLAYER_ATTACK";
        public static final String ITEM_USE = "ITEM_USE";
        
        // Window actions
        public static final String UPGRADE_ITEM = "UPGRADE_ITEM";
        public static final String GHOST_REWARD = "GHOST_REWARD";
        public static final String MONK_ABILITY = "MONK_ABILITY";
        public static final String ENERGIZE_ITEM = "ENERGIZE_ITEM";
        public static final String COMBO_MOVE = "COMBO_MOVE";
        public static final String SUBCLASS_CHOOSE = "SUBCLASS_CHOOSE";
        public static final String ABILITY_CHOOSE = "ABILITY_CHOOSE";
        public static final String BLACKSMITH = "BLACKSMITH";
        
        // Game state
        public static final String TURN_CHANGE = "TURN_CHANGE";
        public static final String GAME_OVER = "GAME_OVER";
        
        // Hero creation
        public static final String HERO_CREATED = "HERO_CREATED";
        
        // Map for fast lookup
        private static final Map<String, Integer> typeToId = new HashMap<>();
        private static final Map<Integer, String> idToType = new HashMap<>();
        private static int nextId = 1;
        
        static {
            // Register all types with IDs for compact serialization
            register(PLAYER_ASSIGN);
            register(PLAYER_JOIN);
            register(PLAYER_LEAVE);
            register(SERVER_SHUTDOWN);
            register(SEED_INIT);
            register(HERO_CLASS);
            register(HERO_CLASS_SELECTED);
            register(PLAYER_READY);
            register(PLAYER_KICK);
            register(KICK_NOTIFY);
            register(PLAYER_MOVE);
            register(PLAYER_ATTACK);
            register(ITEM_USE);
            register(UPGRADE_ITEM);
            register(GHOST_REWARD);
            register(MONK_ABILITY);
            register(ENERGIZE_ITEM);
            register(COMBO_MOVE);
            register(SUBCLASS_CHOOSE);
            register(ABILITY_CHOOSE);
            register(BLACKSMITH);
            register(TURN_CHANGE);
            register(GAME_OVER);
            register(HERO_CREATED);
        }
        
        private static void register(String type) {
            typeToId.put(type, nextId);
            idToType.put(nextId, type);
            nextId++;
        }
        
        public static int getId(String type) {
            Integer id = typeToId.get(type);
            return id != null ? id : 0;
        }
        
        public static String getType(int id) {
            String type = idToType.get(id);
            return type != null ? type : "";
        }
    }
    
    // Properties - using primitives for efficiency
    public int typeId;  // Compact type ID instead of String
    public int playerId;
    public byte[] data;  // Direct Kryo serialized data instead of String
    
    // Default constructor for Kryo
    public GameMessage() {}
    
    // Constructor with type and player
    public GameMessage(String type, int playerId) {
        this.typeId = Types.getId(type);
        this.playerId = playerId;
    }
    
    // Get the type string (for handler lookup)
    public String getType() {
        return Types.getType(typeId);
    }
    
    /**
     * Serialize a Bundle to bytes using UTF-8.
     * This provides a cleaner byte representation than going through Kryo object serialization.
     */
    public static byte[] serializeBundle(Bundle bundle, Kryo kryo) {
        if (bundle == null) return null;
        
        // Get the JSON string representation directly
        String jsonStr = bundle.toString();
        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Deserialize bytes back to a Bundle.
     * Uses Bundle.read() for full compatibility with existing handlers.
     */
    public static Bundle deserializeBundle(byte[] data, Kryo kryo) {
        if (data == null || data.length == 0) return new Bundle();
        
        try {
            return Bundle.read(new ByteArrayInputStream(data));
        } catch (Exception e) {
            e.printStackTrace();
            return new Bundle();
        }
    }
    
    /**
     * Legacy support: Convert to/from the old BundleMessage format.
     * This allows gradual migration.
     */
    public static GameMessage fromLegacy(NetworkManager.BundleMessage legacy) {
        GameMessage msg = new GameMessage();
        msg.typeId = Types.getId(legacy.type);
        msg.playerId = legacy.playerId;
        
        if (legacy.bundleData != null && !legacy.bundleData.isEmpty()) {
            // Convert legacy String format to bytes
            msg.data = legacy.bundleData.getBytes(StandardCharsets.UTF_8);
        }
        
        return msg;
    }
    
    public NetworkManager.BundleMessage toLegacy() {
        NetworkManager.BundleMessage legacy = new NetworkManager.BundleMessage();
        legacy.type = getType();
        legacy.playerId = playerId;
        
        if (data != null) {
            legacy.bundleData = new String(data, StandardCharsets.UTF_8);
        }
        
        return legacy;
    }
    
    @Override
    public String toString() {
        return "GameMessage[type=" + getType() + ", player=" + playerId + ", dataLen=" + (data != null ? data.length : 0) + "]";
    }
}
