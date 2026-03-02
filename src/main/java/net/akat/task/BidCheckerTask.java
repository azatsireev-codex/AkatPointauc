package net.akat.task;

import net.akat.ActiveBid;
import net.akat.Main;
import net.akat.managers.BalanceManager;
import net.akat.managers.BidManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class BidCheckerTask implements Runnable {

    private final OkHttpClient client = new OkHttpClient();
    private final BidManager bidManager;
    private final BalanceManager balanceManager;
    private final Main plugin;

    public BidCheckerTask(BidManager bidManager, BalanceManager balanceManager) {
        this.bidManager = bidManager;
        this.balanceManager = balanceManager;
        this.plugin = Main.getInstance();
    }

    @Override
    public void run() {
        for (Map.Entry<String, ActiveBid> entry : bidManager.getActiveBids().entrySet()) {
            String bidId = entry.getKey();
            ActiveBid bid = entry.getValue();

            Request request = new Request.Builder()
                    .url("https://pointauc.com/api/oshino/bid/status?id=" + bidId)
                    .get()
                    .addHeader("Authorization", plugin.pointaucToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) continue;

                String body = response.body().string();

                if (body.contains("\"status\":\"rejected\"")) {
                    returnPoints(bid);
                } else if (body.contains("\"status\":\"processed\"") || body.contains("\"status\":\"notFound\"")) {
                    bidManager.removeBid(bidId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void returnPoints(ActiveBid bid) {
        bidManager.removeBid(bid.bidId);
        balanceManager.addPoints(bid.playerUUID, bid.amount);

        saveBalanceToDatabase(bid.playerUUID);

        Player player = Bukkit.getPlayer(bid.playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage("§cВаша ставка была отклонена стримером. Баллы возвращены.");
        }
    }

    private void saveBalanceToDatabase(UUID playerUUID) {
        try {
            int points = balanceManager.getPoints(playerUUID);

            plugin.database.save(playerUUID, points);

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить баланс для " + playerUUID +
                    " после возврата ставки: " + e.getMessage());

            scheduleDelayedSave(playerUUID);
        }
    }

    private void scheduleDelayedSave(UUID playerUUID) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                int points = balanceManager.getPoints(playerUUID);
                plugin.database.save(playerUUID, points);
                plugin.getLogger().info("Баланс для " + playerUUID + " успешно сохранен с задержкой");
            } catch (Exception e) {
                plugin.getLogger().severe("Не удалось сохранить баланс для " + playerUUID + " даже с задержкой");
            }
        }, 100L);
    }
}
