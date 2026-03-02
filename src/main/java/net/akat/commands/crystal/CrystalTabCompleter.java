package net.akat.commands.crystal;

import net.akat.crystal.CrystalManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CrystalTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "list", "resetcooldown");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return CrystalManager.getInstance().getCrystalEntities().keySet().stream()
                    .map(String::valueOf)
                    .sorted()
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("resetcooldown")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }

        return Collections.emptyList();
    }
}
