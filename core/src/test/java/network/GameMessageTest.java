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
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.*;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameMessage serialization.
 */
public class GameMessageTest {

    private Kryo kryo;

    @BeforeEach
    public void setup() {
        kryo = new Kryo();
        kryo.register(GameMessage.class);
        kryo.register(HashMap.class);
    }

    @Test
    public void testMessageTypesRegistered() {
        // Verify all message types are registered
        assertTrue(GameMessage.Types.getId("PLAYER_JOIN") > 0);
        assertTrue(GameMessage.Types.getId("PLAYER_LEAVE") > 0);
        assertTrue(GameMessage.Types.getId("PLAYER_READY") > 0);
        assertTrue(GameMessage.Types.getId("PLAYER_MOVE") > 0);
        assertTrue(GameMessage.Types.getId("PLAYER_ATTACK") > 0);
        assertTrue(GameMessage.Types.getId("HERO_CLASS") > 0);
        assertTrue(GameMessage.Types.getId("TURN_CHANGE") > 0);
        assertTrue(GameMessage.Types.getId("GAME_OVER") > 0);
    }

    @Test
    public void testTypeIdConsistency() {
        // Same type should always return same ID
        int id1 = GameMessage.Types.getId("PLAYER_MOVE");
        int id2 = GameMessage.Types.getId("PLAYER_MOVE");
        assertEquals(id1, id2);
    }

    @Test
    public void testTypeToIdAndBack() {
        String originalType = "PLAYER_ATTACK";
        int id = GameMessage.Types.getId(originalType);
        String recoveredType = GameMessage.Types.getType(id);
        
        assertEquals(originalType, recoveredType);
    }

    @Test
    public void testUnknownTypeReturnsZero() {
        int id = GameMessage.Types.getId("UNKNOWN_MESSAGE_TYPE");
        assertEquals(0, id);
    }

    @Test
    public void testUnknownIdReturnsEmpty() {
        String type = GameMessage.Types.getType(99999);
        assertEquals("", type);
    }

    @Test
    public void testGameMessageCreation() {
        GameMessage msg = new GameMessage("PLAYER_MOVE", 1);
        
        assertEquals("PLAYER_MOVE", msg.getType());
        assertEquals(1, msg.playerId);
    }

    @Test
    public void testGameMessageDefaultConstructor() {
        GameMessage msg = new GameMessage();
        // Default values should be zero/null
        assertEquals(0, msg.typeId);
        assertEquals(0, msg.playerId);
        assertNull(msg.data);
    }

    @Test
    public void testSerializeBundle() {
        Bundle bundle = new Bundle();
        bundle.put("dst", 100);
        bundle.put("playerId", 1);
        
        byte[] serialized = GameMessage.serializeBundle(bundle, kryo);
        
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    @Test
    public void testSerializeNullBundle() {
        byte[] serialized = GameMessage.serializeBundle(null, kryo);
        assertNull(serialized);
    }

    @Test
    public void testDeserializeBundle() {
        Bundle original = new Bundle();
        original.put("dst", 100);
        original.put("playerId", 1);
        original.put("action", "move");
        
        byte[] serialized = GameMessage.serializeBundle(original, kryo);
        Bundle deserialized = GameMessage.deserializeBundle(serialized, kryo);
        
        assertNotNull(deserialized);
        assertEquals(100, deserialized.getInt("dst"));
        assertEquals(1, deserialized.getInt("playerId"));
        assertEquals("move", deserialized.getString("action"));
    }

    @Test
    public void testDeserializeEmptyData() {
        Bundle result = GameMessage.deserializeBundle(null, kryo);
        assertNotNull(result);
        
        result = GameMessage.deserializeBundle(new byte[0], kryo);
        assertNotNull(result);
    }

    @Test
    public void testRoundTrip() {
        // Create original message
        GameMessage original = new GameMessage("PLAYER_ATTACK", 5);
        
        // Add data
        Bundle bundle = new Bundle();
        bundle.put("targetId", 42);
        original.data = GameMessage.serializeBundle(bundle, kryo);
        
        // Verify
        assertEquals("PLAYER_ATTACK", original.getType());
        assertEquals(5, original.playerId);
        
        // Deserialize data
        Bundle recovered = GameMessage.deserializeBundle(original.data, kryo);
        assertEquals(42, recovered.getInt("targetId"));
    }

    @Test
    public void testLegacyConversion() {
        // Create legacy message
        NetworkManager.BundleMessage legacy = new NetworkManager.BundleMessage("PLAYER_MOVE", 10);
        legacy.bundleData = "{\"dst\":50}";
        
        // Convert to new format
        GameMessage converted = GameMessage.fromLegacy(legacy);
        
        assertEquals("PLAYER_MOVE", converted.getType());
        assertEquals(10, converted.playerId);
        assertNotNull(converted.data);
    }

    @Test
    public void testToLegacy() {
        // Create new message
        GameMessage msg = new GameMessage("PLAYER_ATTACK", 3);
        
        // Convert to legacy
        NetworkManager.BundleMessage legacy = msg.toLegacy();
        
        assertEquals("PLAYER_ATTACK", legacy.type);
        assertEquals(3, legacy.playerId);
    }

    @Test
    public void testToString() {
        GameMessage msg = new GameMessage("PLAYER_MOVE", 7);
        msg.data = new byte[]{1, 2, 3, 4};
        
        String str = msg.toString();
        
        assertTrue(str.contains("PLAYER_MOVE"));
        assertTrue(str.contains("7"));
    }
}
