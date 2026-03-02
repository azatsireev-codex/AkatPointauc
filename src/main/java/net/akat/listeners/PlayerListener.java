package net.akat.listeners;

import net.akat.Main;
import net.akat.crystal.CrystalManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        var uuid = p.getUniqueId();

        int points = Main.instance.database.load(uuid);
        Main.instance.balanceManager.setPoints(uuid, points, false);
        Main.instance.giftManager.enableFarming(uuid);
        Main.instance.giftManager.addPlayerToGiftTime(uuid);
        Main.instance.giftManager.addPlayerToPointsTime(uuid);

        CrystalManager.getInstance().onPlayerJoin(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        var uuid = p.getUniqueId();

        int points = Main.instance.balanceManager.getPoints(uuid);
        Main.instance.database.save(uuid, points);
        Main.instance.giftManager.removePlayerFromGiftTime(uuid);
        Main.instance.giftManager.removePlayerFromPointsTime(uuid);
    }
}
