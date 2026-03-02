package net.akat.managers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceManager {
    public Map<UUID, Integer> balances = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyBalances = ConcurrentHashMap.newKeySet();

    public int getPoints(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }

    public void addPoints(UUID uuid, int amount) {
        balances.put(uuid, getPoints(uuid) + amount);
        dirtyBalances.add(uuid);
    }

    public boolean removePoints(UUID uuid, int amount) {
        if (getPoints(uuid) >= amount) {
            balances.put(uuid, getPoints(uuid) - amount);
            dirtyBalances.add(uuid);
            return true;
        }
        return false;
    }

    public void setPoints(UUID uuid, int amount) {
        setPoints(uuid, amount, true);
    }

    public void setPoints(UUID uuid, int amount, boolean markDirty) {
        balances.put(uuid, amount);
        if (markDirty) {
            dirtyBalances.add(uuid);
        }
    }

    public Set<UUID> consumeDirtyBalances() {
        Set<UUID> snapshot = new HashSet<>(dirtyBalances);
        dirtyBalances.removeAll(snapshot);
        return snapshot;
    }
}
