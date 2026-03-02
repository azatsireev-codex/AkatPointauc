package net.akat.listeners;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import net.akat.Main;
import net.akat.crystal.CrystalManager;
import net.akat.crystal.CrystalPoint;
import net.akat.crystal.data.PlayerCrystalData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class CrystalClickListener implements Listener {

    private static final double CHECK_RADIUS = 1.0;
    private static final double HEIGHT_OFFSET = 1.0;

    private final CrystalManager manager = CrystalManager.getInstance();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        PlayerCrystalData data = manager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        Location playerLoc = player.getLocation();
        List<CrystalPoint> crystals = manager.getCrystals();

        for (int i = 0; i < crystals.size(); i++) {
            if (data.isCollected(i)) continue;

            CrystalPoint point = crystals.get(i);
            Location crystalLoc = point.toLocation();

            for (double yOffset = 0.0; yOffset <= HEIGHT_OFFSET; yOffset += 0.5) {
                Location checkLoc = crystalLoc.clone().add(0, yOffset, 0);

                if (player.getWorld().equals(checkLoc.getWorld()) &&
                        playerLoc.distance(checkLoc) <= CHECK_RADIUS) {
                    manager.onCrystalCollect(player, i);
                    break;
                }
            }
        }
    }
}
