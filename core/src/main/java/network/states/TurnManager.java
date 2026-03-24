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

package network.states;

import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages turn-based gameplay for multiplayer.
 * Tracks whose turn it is and allows actions only from the active player.
 */
public class TurnManager {
    
    private static TurnManager instance;
    
    // Current turn state
    private int currentPlayerIndex = 0;
    private List<Integer> playerOrder = new ArrayList<>();
    private int turnNumber = 1;
    private boolean turnInProgress = false;
    
    // Turn timeout (in seconds) - if player takes too long
    private static final int TURN_TIMEOUT = 30;
    private float timeRemaining = TURN_TIMEOUT;
    
    private TurnManager() {}
    
    public static TurnManager getInstance() {
        if (instance == null) instance = new TurnManager();
        return instance;
    }
    
    /**
     * Initialize turn order based on connected players.
     * Called when game starts.
     */
    public void initTurns() {
        playerOrder.clear();
        for (Multiplayer.PlayerInfo p : Multiplayer.Players.getAll()) {
            if (p.hero != null && p.hero.isAlive()) {
                playerOrder.add(p.connectionID);
            }
        }
        
        currentPlayerIndex = 0;
        turnNumber = 1;
        turnInProgress = false;
        
        if (!playerOrder.isEmpty()) {
            broadcastTurnChange();
        }
    }
    
    /**
     * Check if it's a specific player's turn.
     */
    public boolean isPlayerTurn(int playerId) {
        if (playerOrder.isEmpty()) return false;
        return playerOrder.get(currentPlayerIndex) == playerId;
    }
    
    /**
     * Get the ID of the current player whose turn it is.
     */
    public int getCurrentPlayerId() {
        if (playerOrder.isEmpty()) return -1;
        return playerOrder.get(currentPlayerIndex);
    }
    
    /**
     * Check if a player can act right now.
     * Returns true only if it's that player's turn and turn is not already in progress.
     */
    public boolean canPlayerAct(int playerId) {
        return isPlayerTurn(playerId) && !turnInProgress;
    }
    
    /**
     * Mark that a player has started their turn (sent an action).
     * Returns true if action was accepted, false if not player's turn.
     */
    public boolean playerStartedAction(int playerId) {
        if (!canPlayerAct(playerId)) {
            return false;
        }
        turnInProgress = true;
        return true;
    }
    
    /**
     * End the current player's turn and advance to next player.
     * Called when player's action is processed.
     */
    public void endTurn() {
        if (playerOrder.isEmpty()) return;
        
        turnInProgress = false;
        
        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();
        
        // If we wrapped around, increment turn number
        if (currentPlayerIndex == 0) {
            turnNumber++;
        }
        
        // Reset turn timer
        timeRemaining = TURN_TIMEOUT;
        
        // Broadcast new turn to all clients
        broadcastTurnChange();
    }
    
    /**
     * Skip current player's turn (e.g., if they disconnected or AFK).
     */
    public void skipCurrentTurn() {
        if (playerOrder.isEmpty()) return;
        
        // Remove current player from order if they're dead
        int currentPlayerId = playerOrder.get(currentPlayerIndex);
        Multiplayer.PlayerInfo p = Multiplayer.Players.get(currentPlayerId);
        if (p == null || p.hero == null || !p.hero.isAlive()) {
            playerOrder.remove(currentPlayerIndex);
            if (playerOrder.isEmpty()) {
                // Game over - no alive players
                broadcastGameOver();
                return;
            }
            // Adjust index if we removed the last element
            if (currentPlayerIndex >= playerOrder.size()) {
                currentPlayerIndex = 0;
            }
        }
        
        turnInProgress = false;
        timeRemaining = TURN_TIMEOUT;
        broadcastTurnChange();
    }
    
    /**
     * Update timer - call from game loop.
     * Returns true if timeout occurred.
     */
    public boolean update(float elapsed) {
        if (playerOrder.isEmpty() || !turnInProgress) return false;
        
        timeRemaining -= elapsed;
        if (timeRemaining <= 0) {
            // Timeout - skip this player's turn
            skipCurrentTurn();
            return true;
        }
        return false;
    }
    
    /**
     * Reset turn manager (e.g., when leaving game).
     */
    public void reset() {
        playerOrder.clear();
        currentPlayerIndex = 0;
        turnNumber = 1;
        turnInProgress = false;
        timeRemaining = TURN_TIMEOUT;
    }
    
    // Getters for UI
    public int getTurnNumber() { return turnNumber; }
    public float getTimeRemaining() { return timeRemaining; }
    public boolean isTurnInProgress() { return turnInProgress; }
    public int getPlayerCount() { return playerOrder.size(); }
    
    // Broadcasting
    private void broadcastTurnChange() {
        int currentPlayerId = getCurrentPlayerId();
        
        Bundle bundle = new Bundle();
        bundle.put("turnNumber", turnNumber);
        bundle.put("currentPlayerId", currentPlayerId);
        bundle.put("turnInProgress", turnInProgress);
        bundle.put("timeRemaining", (int)timeRemaining);
        
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("TURN_CHANGE", currentPlayerId);
        msg.bundleData = bundle.toString();
        
        NetworkManager.broadcastMessageServer(msg, null);
    }
    
    private void broadcastGameOver() {
        Bundle bundle = new Bundle();
        bundle.put("reason", "no_alive_players");
        
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("GAME_OVER", -1);
        msg.bundleData = bundle.toString();
        
        NetworkManager.broadcastMessageServer(msg, null);
    }
    
    /**
     * Client-side handler for turn change messages.
     */
    public static class TurnChangeHandler implements MessageHandler {
        @Override
        public String getType() { return "TURN_CHANGE"; }
        
        @Override
        public void msgHandle(int senderId, Bundle bundle) {
            // Update client-side turn state
            // This would update the UI to show whose turn it is
            // and disable input for other players
            com.watabou.noosa.Game.runOnRenderThread(() -> {
                // Update UI or game state
                // The client will handle this based on the received data
            });
        }
    }
    
    /**
     * Client-side handler for game over messages.
     */
    public static class GameOverHandler implements MessageHandler {
        @Override
        public String getType() { return "GAME_OVER"; }
        
        @Override
        public void msgHandle(int senderId, Bundle bundle) {
            com.watabou.noosa.Game.runOnRenderThread(() -> {
                // Handle game over
                // Could show game over screen, stats, etc.
            });
        }
    }
}
