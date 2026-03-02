package net.akat.commands.completers;

import net.akat.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BidTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Main.instance.getAllowedBidAmounts().stream()
                    .map(String::valueOf)
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
