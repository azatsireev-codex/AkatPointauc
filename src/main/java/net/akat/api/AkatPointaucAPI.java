package net.akat.api;

import net.akat.managers.BalanceManager;

import java.util.UUID;

public class AkatPointaucAPI {

    private final BalanceManager balanceManager;

    public AkatPointaucAPI(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    /**
     * Получить текущий баланс игрока.
     * @param uuid UUID игрока.
     * @return количество кубиславов у игрока.
     */
    public int getPoints(UUID uuid) {
        return balanceManager.getPoints(uuid);
    }

    /**
     * Добавить указанное количество кубиславов игроку.
     * @param uuid UUID игрока.
     * @param amount количество кубиславов для добавления.
     */
    public void addPoints(UUID uuid, int amount) {
        balanceManager.addPoints(uuid, amount);
    }

    /**
     * Уменьшить баланс игрока на указанное количество кубиславов.
     * Если у игрока недостаточно кубиславов, операция не выполняется.
     * @param uuid UUID игрока.
     * @param amount количество кубиславов для снятия.
     * @return true, если снятие прошло успешно, false если недостаточно средств.
     */
    public boolean removePoints(UUID uuid, int amount) {
        return balanceManager.removePoints(uuid, amount);
    }

    /**
     * Установить баланс игрока в указанное количество кубиславов.
     * @param uuid UUID игрока.
     * @param amount новое количество кубиславов.
     */
    public void setPoints(UUID uuid, int amount) {
        balanceManager.setPoints(uuid, amount);
    }
}
