package net.akat.commands;

import net.akat.Database;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ToggleAuctionCommand implements CommandExecutor {

    private final Database database;

    public ToggleAuctionCommand(Database database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("pointauc.toggleauction")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
            return true;
        }

        boolean current = database.isAuctionEnabled();
        boolean newState = !current;

        database.setAuctionEnabled(newState);

        if (newState) {
            sender.sendMessage(ChatColor.GREEN + "Аукцион включён. Ставки теперь принимаются.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Аукцион отключён. Ставки временно не принимаются.");
        }

        return true;
    }
}
