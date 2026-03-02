package net.akat.commands;

import net.akat.managers.GiftManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GiftClaimCommand implements CommandExecutor {

    private final GiftManager giftManager;

    public GiftClaimCommand(GiftManager giftManager) {
        this.giftManager = giftManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        UUID uuid = player.getUniqueId();
        if (giftManager.claim(uuid)) {
            player.sendMessage("§aВы получили §b+25 Кубиславов§a и продолжили фарм!");
        } else {
            player.sendMessage("§cСейчас нет активного подарка или вы уже его использовали.");
        }
        return true;
    }
}
