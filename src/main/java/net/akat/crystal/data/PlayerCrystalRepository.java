package net.akat.crystal.data;

import java.io.*;
import java.util.BitSet;
import java.util.UUID;

public class PlayerCrystalRepository {
    private final File folder;

    public PlayerCrystalRepository(File folder) {
        this.folder = folder;
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerCrystalData load(UUID uuid) {
        File file = new File(folder, uuid + ".dat");
        PlayerCrystalData data = new PlayerCrystalData();
        if (!file.exists()) return data;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            data.setLastResetTime(in.readLong());
            byte[] bytes = in.readAllBytes();
            data.getCollected().clear();
            data.getCollected().or(BitSet.valueOf(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
        data.resetIfExpired();
        return data;
    }

    public void save(UUID uuid, PlayerCrystalData data) {
        File file = new File(folder, uuid + ".dat");

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeLong(data.getLastResetTime());
            out.write(data.getCollected().toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
