package net.akat.commands;

import net.akat.ActiveBid;
import net.akat.Main;
import okhttp3.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BidCommand implements CommandExecutor {

    private final OkHttpClient client = new OkHttpClient();
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // Задержка между ставками

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (!(sender instanceof Player player)) return false;

        UUID uuid = player.getUniqueId();

        if (!Main.instance.database.isAuctionEnabled()) {
            player.sendMessage("§cАукцион в данный момент не принимает ставки, следите за стримами Slaffneft.");
            return true;
        }

        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < 5000) {
            long secondsLeft = (5000 - (now - cooldowns.get(uuid))) / 1000;
            player.sendMessage("§cПодождите " + secondsLeft + " сек. перед следующей ставкой.");
            return true;
        }
        cooldowns.put(uuid, now);

        if (args.length < 2) {
            player.sendMessage("§cИспользование: /bid <сумма> <сообщение>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cВведите корректную сумму.");
            return true;
        }

        if (!Main.instance.getAllowedBidAmounts().contains(amount)) {
            player.sendMessage("§cНедопустимая сумма. Доступные суммы: " +
                    Main.instance.getAllowedBidAmounts().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        var balanceManager = Main.instance.balanceManager;
        var bidManager = Main.instance.bidManager;

        if (!balanceManager.removePoints(uuid, amount)) {
            player.sendMessage("§cНедостаточно баллов.");
            return true;
        }

        player.sendMessage("§eОтправка ставки...");

        UUID bidId = UUID.randomUUID();

        String json = String.format(
                "{\"bids\":[{\"cost\":%d,\"insertStrategy\":\"auto\",\"username\":\"%s\",\"message\":\"%s\",\"id\":\"%s\",\"color\":\"#f58e8e\",\"isDonation\":false,\"timestamp\":\"%s\",\"investorId\":\"%s\"}]}",
                amount,
                player.getName(),
                message,
                bidId,
                Instant.now(),
                uuid
        );

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);

        Request request = new Request.Builder()
                .url("https://pointauc.com/api/oshino/bids")
                .post(body)
                .addHeader("Authorization", Main.instance.pointaucToken)
                .addHeader("Content-Type", "application/json")
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Ставка не удалась. Код ошибки: " + response.code());
                }

                player.sendMessage("§aСтавка успешно отправлена!");

                bidManager.addBid(new ActiveBid(uuid, bidId.toString(), amount));
            } catch (Exception e) {
                balanceManager.addPoints(uuid, amount); // Возврат баллов
                player.sendMessage("§cНе удалось отправить ставку. Баллы возвращены.");
                e.printStackTrace();
            }
        }).start();

        return true;
    }
}
