package net.akat.commands;

import net.akat.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadPointaucCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pointauc.reload")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }

        Main.instance.reloadPointaucConfig();
        sender.sendMessage("§aКонфигурация Pointauc перезагружена.");
        return true;
    }
}
