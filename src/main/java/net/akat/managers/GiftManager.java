package net.akat.managers;

import net.akat.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GiftManager {

    private final Map<UUID, Long> lastGiftTime = new HashMap<>();
    private final Map<UUID, Long> lastPointsTime = new HashMap<>();
    private final Set<UUID> awaitingClick = new HashSet<>();
    private final Set<UUID> canFarm = new HashSet<>();

    public void start() {
        if (Main.isFolia()) {
            // Для основного цикла по всем игрокам используем GlobalRegionScheduler
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Main.getInstance(), task -> {
                if (!Main.getInstance().isAfkFarmEnabled()) return;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - lastGiftTime.getOrDefault(uuid, 0L) >= 30 * 60 * 1000L) {
                        disableFarming(uuid);
                        awaitingClick.add(uuid);

                        TextComponent msg = new TextComponent("§d🎁 Нажми здесь, чтобы получить §b+25 Кубиславов!");
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/giftclaim"));
                        player.spigot().sendMessage(msg);

                        lastGiftTime.put(uuid, currentTime);

                        // Для задачи, связанной с конкретным игроком/локацией, используем RegionScheduler
                        Bukkit.getRegionScheduler().runDelayed(Main.getInstance(), player.getLocation(), task2 -> {
                            if (awaitingClick.contains(uuid)) {
                                awaitingClick.remove(uuid);
                                player.sendMessage("§cВы не успели нажать на подарок. Фарм Кубиславов приостановлен.");
                            }
                        }, 20L * 60);
                    }

                    if (canFarm.contains(uuid)) {
                        if (currentTime - lastPointsTime.getOrDefault(uuid, 0L) >= 5 * 60 * 1000L) {
                            Main.getInstance().balanceManager.addPoints(uuid, 5);
                            lastPointsTime.put(uuid, currentTime);
                        }
                    }
                }
            }, 1L, 20L * 60);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!Main.getInstance().isAfkFarmEnabled()) return;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();
                        long currentTime = System.currentTimeMillis();

                        if (currentTime - lastGiftTime.getOrDefault(uuid, 0L) >= 30 * 60 * 1000L) {
                            disableFarming(uuid);
                            awaitingClick.add(uuid);

                            TextComponent msg = new TextComponent("§d🎁 Нажми здесь, чтобы получить §b+25 Кубиславов!");
                            msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/giftclaim"));
                            player.spigot().sendMessage(msg);

                            lastGiftTime.put(uuid, currentTime);

                            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                                if (awaitingClick.contains(uuid)) {
                                    awaitingClick.remove(uuid);
                                    player.sendMessage("§cВы не успели нажать на подарок. Фарм Кубиславов приостановлен.");
                                }
                            }, 20L * 60); // Исправлено: 60 секунд, а не 30
                        }

                        if (canFarm.contains(uuid)) {
                            if (currentTime - lastPointsTime.getOrDefault(uuid, 0L) >= 5 * 60 * 1000L) {
                                Main.getInstance().balanceManager.addPoints(uuid, 5);
                                lastPointsTime.put(uuid, currentTime);
                            }
                        }
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 20L * 60);
        }
    }

    public boolean claim(UUID uuid) {
        if (awaitingClick.remove(uuid)) {
            Main.getInstance().balanceManager.addPoints(uuid, 25);
            enableFarming(uuid);
            return true;
        }
        return false;
    }

    public boolean isAwaiting(UUID uuid) {
        return awaitingClick.contains(uuid);
    }

    public void enableFarming(UUID uuid) {
        canFarm.add(uuid);
    }

    public void disableFarming(UUID uuid) {
        canFarm.remove(uuid);
    }

    public boolean isFarming(UUID uuid) {
        return canFarm.contains(uuid);
    }

    public void addPlayerToGiftTime(UUID uuid) {
        if (!lastGiftTime.containsKey(uuid)) {
            lastGiftTime.put(uuid, System.currentTimeMillis());
        }
    }

    public void removePlayerFromGiftTime(UUID uuid) {
        lastGiftTime.remove(uuid);
    }

    public void addPlayerToPointsTime(UUID uuid) {
        if (!lastPointsTime.containsKey(uuid)) {
            lastPointsTime.put(uuid, System.currentTimeMillis());
        }
    }

    public void removePlayerFromPointsTime(UUID uuid) {
        lastPointsTime.remove(uuid);
    }
}
