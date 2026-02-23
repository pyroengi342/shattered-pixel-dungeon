package network;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;

public class Multiplayer {
    public static boolean isMultiplayer = false;
    public static boolean isHost = false;
    // int maxPlayersCount = MPSettings.maxPlayers();
    private static final PlayerContainer playerContainer = new PlayerContainer();
    public static class PlayerInfo {
        public int id;
        public String name;

        //public int heroClass;
        public Hero hero;  // Ссылка на героя в игре
        public boolean isLocal; // Является ли локальным игроком
        public PlayerInfo() {}
        public PlayerInfo(int id, String name) {
            this.id = id;
            this.name = name;
            this.isLocal = false;
        }
    }
    private static class PlayerContainer {
        private final Map<Integer, PlayerInfo> players = new LinkedHashMap<>();

        public void add(PlayerInfo player) {
            players.put(player.id, player);
        }

        public PlayerInfo get(int id) {
            return players.get(id);
        }

        public void remove(int id) {
            players.remove(id);
        }

        public List<PlayerInfo> getAll() {
            return new ArrayList<>(players.values());
        }

        public void clear() {
            players.clear();
        }

        public int size() {
            return players.size();
        }

        public Hero getHero(int id) {
            PlayerInfo player = players.get(id);
            return player != null ? player.hero : null;
        }
        public void setHeroClass(int playerId, HeroClass heroClassName) {
            PlayerInfo player = players.get(playerId);
            if (player != null) {
                player.hero.heroClass = heroClassName;
            }
        }
    }
    public static class Players {
        public static void add(PlayerInfo player) {
            playerContainer.add(player);
        }

        public static PlayerInfo get(int id) {
            return playerContainer.get(id);
        }

        public static void remove(int id) {
            playerContainer.remove(id);
        }

        public static List<PlayerInfo> getAll() {
            return playerContainer.getAll();
        }

        public static void clear() {
            playerContainer.clear();
        }
        public static int getPlayerCount() {
            return playerContainer.size();
        }

        public static Hero getHero(int id) {
            PlayerInfo player = playerContainer.get(id);
            return player != null ? player.hero : null;
        }
        public static void setHeroClass(int playerId, HeroClass heroClassName) {
            PlayerInfo player = playerContainer.get(playerId);
            if (player.hero != null) {
                player.hero.heroClass = heroClassName;
            }
        }
    }

    public static <T extends Item> T findItemInAllHeroes(Class<T> itemClass) {
            for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
                if (player.hero != null && player.hero.isAlive()) {
                    T item = player.hero.belongings.getItem(itemClass);
                    if (item != null) {
                        return item;
                    }
                }
            }
            return null;
            // Fallback
    }

    // Или метод для получения списка всех предметов у всех героев
    public static <T extends Item> List<T> findAllItemsInAllHeroes(Class<T> itemClass) {
        List<T> result = new ArrayList<>();

            for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
                if (player.hero != null && player.hero.isAlive()) {
                    T item = player.hero.belongings.getItem(itemClass);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }

        return result;
    }

    public static boolean hasAnyHeroTalent(Talent talent) {
            for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
                if (player.hero != null && player.hero.isAlive() && player.hero.hasTalent(talent)) {
                    return true;
                }
            }
            return false;
    }

    public static List<Integer> calculateHeroPositions(Level level, int basePos, int heroCount) {
        List<Integer> positions = new ArrayList<>();

        if (heroCount == 1) {
            positions.add(basePos);
            return positions;
        }

        // Начинаем с базовой позиции
        positions.add(basePos);

        // Ищем соседние свободные клетки для остальных героев
        int found = 1;
        int radius = 1;

        while (found < heroCount && radius <= 3) {
            // Проверяем все клетки в заданном радиусе
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) == radius || Math.abs(dy) == radius) {
                        int cell = basePos + dx + dy * level.width();

                        if (cell >= 0 && cell < level.length() &&
                                !level.invalidHeroPos(cell) &&
                                !positions.contains(cell) &&
                                Actor.findChar(cell) == null) {

                            positions.add(cell);
                            found++;

                            if (found >= heroCount) {
                                return positions;
                            }
                        }
                    }
                }
            }
            radius++;
        }

        // Если не нашли достаточно свободных клеток, возвращаем те, что нашли
        return positions;
    }
}

