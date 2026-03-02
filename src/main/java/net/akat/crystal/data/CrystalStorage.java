package net.akat.crystal.data;

import net.akat.crystal.CrystalPoint;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrystalStorage {
    private final List<CrystalPoint> points = new ArrayList<>();

    public void load(File file) throws IOException {
        points.clear();
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            short count = in.readShort();
            for (int i = 0; i < count; i++) {
                byte worldId = in.readByte();
                float x = in.readFloat();
                float y = in.readFloat();
                float z = in.readFloat();
                points.add(new CrystalPoint(worldId, x, y, z));
            }
        }
    }

    /** Возвращает неизменяемый список для безопасного чтения */
    public List<CrystalPoint> getAll() {
        return Collections.unmodifiableList(points);
    }

    /** Добавление кристалла */
    public void add(CrystalPoint point) {
        points.add(point);
    }

    /** Удаление кристалла по индексу */
    public boolean remove(int index) {
        if (index < 0 || index >= points.size()) return false;
        points.remove(index);
        return true;
    }
}
