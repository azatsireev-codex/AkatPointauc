package net.akat.commands;

import net.akat.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BidAmountCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender.hasPermission("bid.manage"))) {
            sender.sendMessage("§cНет прав.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("§cИспользование: /" + label + " <add|remove> <сумма>");
            return true;
        }

        String action = args[0].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cСумма должна быть числом.");
            return true;
        }

        List<Integer> amounts = Main.instance.getConfig().getIntegerList("allowed-bid-amounts");

        switch (action) {
            case "add":
                if (!amounts.contains(amount)) {
                    amounts.add(amount);
                    sender.sendMessage("§aСумма " + amount + " добавлена.");
                } else {
                    sender.sendMessage("§eСумма уже есть в списке.");
                }
                break;

            case "remove":
                if (amounts.remove((Integer) amount)) {
                    sender.sendMessage("§aСумма " + amount + " удалена.");
                } else {
                    sender.sendMessage("§eСуммы нет в списке.");
                }
                break;

            default:
                sender.sendMessage("§cДоступные действия: add, remove");
                return true;
        }

        Main.instance.getConfig().set("allowed-bid-amounts", amounts);
        Main.instance.saveConfig();
        Main.instance.reloadBidAmounts();

        return true;
    }

}
