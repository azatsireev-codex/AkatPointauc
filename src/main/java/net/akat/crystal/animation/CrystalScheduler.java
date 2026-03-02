package net.akat.crystal.animation;

import net.akat.Main;
import net.akat.crystal.CrystalManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CrystalScheduler {

    public static void start(JavaPlugin plugin) {
        if (Main.isFolia()) {
            io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            scheduler.runAtFixedRate(plugin, task -> {
                CrystalManager.getInstance().onTick();
            }, 1L, 2L);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                CrystalManager.getInstance().onTick();
            }, 0L, 2L);
        }
    }
}
