package network;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import io.netty.channel.ChannelHandlerContext;

public class MessageDispatcher {
    private final NetworkManager networkManager;
    public MessageDispatcher(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    public void dispatch(NetworkManager.BundleMessage message) {
        Game.runOnRenderThread(() -> {
            Bundle bundle = null;
            if (message.bundleData != null && !message.bundleData.isEmpty()) {
                try {
                    bundle = Bundle.read(new ByteArrayInputStream(
                            message.bundleData.getBytes(StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            switch (message.type) {
                case "GAME_STATE":           handleGameState(bundle); break;
                case "PLAYER_INPUT":         handlePlayerInput(bundle, message.playerId); break;
                case "LEVEL_UPDATE":         handleLevelUpdate(bundle); break;
                case "PLAYER_ASSIGN":        handlePlayerAssign(message.playerId, bundle); break;
                case "PLAYER_JOIN":          handlePlayerJoin(message.playerId, bundle); break;
                case "PLAYER_LEAVE":         handlePlayerLeave(message.playerId); break;
                case "SERVER_SHUTDOWN":      handleServerShutdown(); break;
                case "SEED_INIT":            handleSeedInit(bundle); break;
                case "HERO_CLASS":           handleHeroClass(message.playerId, bundle); break;
            }
        });
    }
    private void handlePlayerAssign(int assignedId, Bundle bundle) {
        // Устанавливаем localPlayerId для этого клиента
        NetworkManager.setLocalPlayerId(assignedId);

        // Создаем локального игрока
        String name = bundle.getString("name");
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(assignedId, name);
        player.isLocal = true;
        Multiplayer.Players.add(player);

        System.out.println("Assigned as player: " + name + " (ID: " + assignedId + ")");
    }
    private void handlePlayerInput(Bundle bundle, int playerId) {
        if (bundle != null) {
            String action = bundle.getString("action");
            // обработка действия
        }
    }

    private void handlePlayerJoin(int playerId, Bundle bundle) {
        if (playerId == networkManager.getLocalPlayerId()) {
            return;
        }

        String name = bundle != null && bundle.contains("name")
                ? bundle.getString("name")
                : "Player " + playerId;

        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(playerId, name);
        player.isLocal = false;
        Multiplayer.Players.add(player);

        System.out.println("Player joined: " + name + " (ID: " + playerId + ")");
    }

    private void handlePlayerLeave(int playerId) {
        Multiplayer.Players.remove(playerId);
    }

    private void handleServerShutdown() {
        networkManager.showMessage("Server has been shut down");
        networkManager.disconnect();
    }

    private void handleSeedInit(Bundle bundle) {
        if (bundle != null) {
            long receivedSeed = bundle.getLong("seed");
            String receivedCustomSeedText = bundle.getString("customSeedText");

            com.shatteredpixel.shatteredpixeldungeon.Dungeon.seed = receivedSeed;
            com.shatteredpixel.shatteredpixeldungeon.Dungeon.customSeedText =
                    receivedCustomSeedText != null ? receivedCustomSeedText : "";

            System.out.println("Received seed: " + receivedSeed +
                    ", custom: " + receivedCustomSeedText);
        }
    }
    private void handleHeroClass(int playerId, Bundle bundle) {
        if (NetworkManager.getMode() == NetworkManager.Mode.NONE) return;
        if (bundle != null && bundle.contains("heroClass")) {
            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            Multiplayer.Players.setHeroClass(playerId, heroClass);
            System.out.println("Player " + playerId + " selected class: " + heroClassName);

            // Если сервер, пересылаем другим клиентам
            if (NetworkManager.getMode() == NetworkManager.Mode.SERVER) {
                networkManager.sendHeroClass(heroClass);
            }
        }
    }

    private void handleLevelUpdate(Bundle bundle) {
        // обновление уровня
    }

    private void handleGameState(Bundle bundle) {
        // синхронизация состояния игры
    }

    // Game State Handling
//    private void handleHeroClass(int playerId, Bundle bundle) {
//        if (bundle != null && bundle.contains("heroClass")) {
//            String heroClassName = bundle.getString("heroClass");
//            HeroClass heroClass = HeroClass.valueOf(heroClassName);
//
//            Multiplayer.PlayerInfo player = Multiplayer.Players.get(playerId);
//            if (player != null) {
//                Multiplayer.Players.setHeroClass(playerId, heroClass);
//                System.out.println("Player " + playerId + " selected class: " + heroClassName);
//
//                if (mode == NetworkManager.Mode.SERVER) {
//                    NetworkManager.BundleMessage classMessage = new NetworkManager.BundleMessage();
//                    classMessage.type = "HERO_CLASS";
//                    classMessage.playerId = playerId;
//                    classMessage.bundleData = bundle.toString();
//
//                    for (ChannelHandlerContext clientCtx : connectedClients.values()) {
//                        if (clientCtx.channel().isActive() && clientCtx.channel().hashCode() != playerId) {
//                            clientCtx.writeAndFlush(classMessage);
//                        }
//                    }
//                }
//            }
//        }
//    }
}