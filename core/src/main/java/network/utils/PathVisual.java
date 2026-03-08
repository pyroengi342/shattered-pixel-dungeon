package network.utils;

import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class PathVisual extends Visual {
    private List<PointF> cells;            // центры клеток в мировых координатах
    private float time = 0;                 // таймер для автоудаления

    private Vertexbuffer vertexBuffer;      // буфер вершин на GPU
    private int numSegments;                 // количество сегментов (длина пути - 1)
    private SmartTexture whiteTex;           // белая текстура 1x1 для окрашивания
    private float thickness;                 // толщина линии
    private int color;                        // цвет в формате 0xAARRGGBB

    /**
     * Создаёт визуализацию пути.
     * @param path список индексов клеток (от героя до цели)
     * @param thickness толщина линии в пикселях мира
     * @param color цвет линии (например, 0x88FFFFFF — полупрозрачный белый)
     */
    public PathVisual(List<Integer> path, float thickness, int color) {
        super(0, 0, 0, 0);
        this.thickness = thickness;
        this.color = color;

        // Преобразуем индексы клеток в мировые координаты центров
        cells = new ArrayList<>();
        for (int cell : path) {
            PointF p = DungeonTilemap.tileCenterToWorld(cell);
            cells.add(p);
        }

        initBuffers();
        initBoundingBox();
    }

    private void initBoundingBox()
    {
        // Вычисляем bounding box, охватывающий все точки пути
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (PointF p : cells) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        // Добавляем отступ на половину толщины, чтобы учесть ширину линии
        float half = thickness / 2f;
        x = minX - half;
        y = minY - half;
        width = maxX - minX + thickness;
        height = maxY - minY + thickness;
    }
    /** Создаёт буфер вершин для всех сегментов пути. */
    private void initBuffers() {
        if (cells.size() < 2) {
            numSegments = 0;
            return;
        }

        numSegments = cells.size() - 1;
        FloatBuffer vertices = Quad.createSet(numSegments); // буфер для numSegments квадов

        for (int i = 0; i < numSegments; i++) {
            PointF p1 = cells.get(i);
            PointF p2 = cells.get(i + 1);

            float dx = p2.x - p1.x;
            float dy = p2.y - p1.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len == 0) continue; // защита от вырожденного сегмента

            // Вектор, перпендикулярный направлению сегмента (для толщины)
            float perpX = -dy / len * thickness / 2f;
            float perpY =  dx / len * thickness / 2f;

            // Четыре вершины квада в порядке: левый-нижний, правый-нижний, правый-верхний, левый-верхний
            float[] quad = new float[16];
            // a (левый нижний)
            quad[0] = p1.x - perpX;
            quad[1] = p1.y - perpY;
            quad[2] = 0; // u
            quad[3] = 0; // v
            // b (правый нижний)
            quad[4] = p1.x + perpX;
            quad[5] = p1.y + perpY;
            quad[6] = 0;
            quad[7] = 0;
            // c (правый верхний)
            quad[8] = p2.x + perpX;
            quad[9] = p2.y + perpY;
            quad[10] = 0;
            quad[11] = 0;
            // d (левый верхний)
            quad[12] = p2.x - perpX;
            quad[13] = p2.y - perpY;
            quad[14] = 0;
            quad[15] = 0;

            vertices.put(quad);
        }

        vertices.flip(); // переводим буфер в режим чтения
        vertexBuffer = new Vertexbuffer(vertices);
        whiteTex = TextureCache.createSolid(0xFFFFFFFF); // общая белая текстура
    }
    private static final float[] IDENTITY = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    @Override
    public void update() {
        super.update();
        time += Game.elapsed;
        if (time > 2f) {
            killAndErase(); // автоматически исчезает через 2 секунды
        }
    }

    @Override
    public void draw() {
        if (numSegments == 0 || vertexBuffer == null) return;

        NoosaScript script = NoosaScript.get();
        whiteTex.bind();
        script.camera(Camera.main);
        script.uModel.valueM4(IDENTITY);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b =  color        & 0xFF;
        script.lighting(r/255f, g/255f, b/255f, a/255f, 0,0,0,0);

        script.drawQuadSet(vertexBuffer, numSegments, 0);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (vertexBuffer != null) {
            vertexBuffer.delete();
            vertexBuffer = null;
        }
        // whiteTex кэшируется в TextureCache, удалять его не нужно
    }
}