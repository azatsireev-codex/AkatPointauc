package net.akat.crystal.data;

import java.util.BitSet;

public class PlayerCrystalData {
    private final BitSet collected = new BitSet();
    private long lastResetTime;

    public boolean isCollected(int index) {
        return collected.get(index);
    }

    public void markCollected(int index) {
        collected.set(index);
    }

    public void resetIfExpired() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > 3L * 24 * 60 * 60 * 1000) {
            collected.clear();
            lastResetTime = now;
        }
    }

    public BitSet getCollected() {
        return collected;
    }

    public void setLastResetTime(long time) {
        this.lastResetTime = time;
    }

    public long getLastResetTime() {
        return lastResetTime;
    }
}
