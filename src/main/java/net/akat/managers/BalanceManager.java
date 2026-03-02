package net.akat.managers;

import java.util.HashMap;
import java.util.UUID;

public class BalanceManager {
    public HashMap<UUID, Integer> balances = new HashMap<>();

    public int getPoints(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }

    public void addPoints(UUID uuid, int amount) {
        balances.put(uuid, getPoints(uuid) + amount);
    }

    public boolean removePoints(UUID uuid, int amount) {
        if (getPoints(uuid) >= amount) {
            balances.put(uuid, getPoints(uuid) - amount);
            return true;
        }
        return false;
    }

    public void setPoints(UUID uuid, int amount) {
        balances.put(uuid, amount);
    }
}
