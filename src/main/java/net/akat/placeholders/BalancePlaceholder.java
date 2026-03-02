package net.akat.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.akat.Main;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BalancePlaceholder extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "kubislav";
    }

    @Override
    public @NotNull String getAuthor() {
        return "2FORWORD2";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("balance")) {
            int balance = Main.getInstance().balanceManager.getPoints(player.getUniqueId());
            return String.valueOf(balance);
        }

        return null;
    }
}
