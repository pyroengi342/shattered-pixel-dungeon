package network;

import static network.NetworkManager.getLocalPlayerId;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Multiplayer {
    public static boolean isMultiplayer = false;
    public static boolean isHost = false;
    // int maxPlayersCount = MPSettings.maxPlayers();
    private static final PlayerContainer playerContainer = new PlayerContainer();
    public static class PlayerInfo {
        public int connectionID;
        public String name;

        //public int heroClass;
        public Hero hero;  // Ссылка на героя в игре
        public boolean isLocal; // Является ли локальным игроком
        public PlayerInfo() {}
        public PlayerInfo(int connectionID, String name) {
            this.connectionID = connectionID;
            this.name = name;
            this.isLocal = false;
        }
    }
    private static class PlayerContainer {
        private final Map<Integer, PlayerInfo> players = new LinkedHashMap<>();

        public void add(PlayerInfo player) {
            players.put(player.connectionID, player);
        }

        public PlayerInfo get(int connectionID) {
            return players.get(connectionID);
        }

        public void remove(int connectionID) {
            players.remove(connectionID);
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

        public Hero getHero(int connectionID) {
            PlayerInfo player = players.get(connectionID);
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

        public static PlayerInfo get(int connectionID) {
            return playerContainer.get(connectionID);
        }

        public static void remove(int connectionID) {
            playerContainer.remove(connectionID);
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

        public static Hero getHero(int connectionID) {
            PlayerInfo player = playerContainer.get(connectionID);
            return player != null ? player.hero : null;
        }
        public static void setHeroClass(int playerId, HeroClass heroClassName) {
            PlayerInfo player = playerContainer.get(playerId);
            if (player.hero != null) {
                player.hero.heroClass = heroClassName;
            }
        }
    }

    /**
     * Returns the player you control via connection connectionID.
     */
    public static Hero localHero() {
        PlayerInfo info = Players.get(getLocalPlayerId());
        if (info == null) return null;
        return info.hero;
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

    public static Hero hasAnyHeroTalent(Talent talent) {
            for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
                if (player.hero != null && player.hero.isAlive() && player.hero.hasTalent(talent)) {
                    return player.hero;
                }
            }
            return null;
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

    /**
     * Находит ближайшего живого героя к указанной позиции.
     * Приоритет отдаётся героям, которые видят эту позицию (fieldOfView[pos] == true).
     * Если таких нет, ищет героев в радиусе closeRadius (по умолчанию 6).
     * @param pos позиция на карте
     * @param closeRadius радиус, в котором герой считается "близким", если не видит
     * @return ближайший подходящий герой или null, если никого нет
     */
    public static Hero findNearestHero(int pos, int closeRadius) {
        List<Hero> heroes = new ArrayList<>();
            for (PlayerInfo info : Players.getAll()) {
                if (info.hero != null && info.hero.isAlive() && info.hero.fieldOfView != null) {
                    heroes.add(info.hero);
                }
            }

        Hero nearestVisible = null;
        int minVisibleDist = Integer.MAX_VALUE;
        Hero nearestClose = null;
        int minCloseDist = Integer.MAX_VALUE;

        for (Hero h : heroes) {
            int dist = Dungeon.level.distance(h.pos, pos);
            if (h.fieldOfView[pos]) {
                if (dist < minVisibleDist) {
                    minVisibleDist = dist;
                    nearestVisible = h;
                }
            } else if (dist < closeRadius && dist < minCloseDist) {
                minCloseDist = dist;
                nearestClose = h;
            }
        }

        // Сначала возвращаем видимого, иначе ближайшего в радиусе
        return nearestVisible != null ? nearestVisible : nearestClose;
    }

    // Перегруженный метод с радиусом по умолчанию 6
    public static Hero findNearestHero(int pos) {
        return findNearestHero(pos, 6);
    }

    public static boolean isVisibleToAnyHero(int cell) {
        for (PlayerInfo info : Players.getAll()) {
            Hero h = info.hero;
            if (h != null && h.isAlive() && h.fieldOfView != null && h.fieldOfView[cell]) {
                return true;
            }
        }
        return false;
    }

    public static void interruptAll(){
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            player.hero.interrupt();
        }
    }
    public static Item findItemById(long id) {
        // Поиск в инвентарях всех героев
        for (Multiplayer.PlayerInfo info : Multiplayer.Players.getAll()) {
            Hero h = info.hero;
            if (h == null) continue;
            for (Item item : h.belongings) {
                if (item.getItemID() == id) return item;
            }
        }
        // Поиск в кучах на уровне
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            for (Item item : heap.items) {
                if (item.getItemID() == id) return item;
            }
        }
        return null;
    }
}

