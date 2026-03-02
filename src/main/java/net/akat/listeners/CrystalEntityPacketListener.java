package net.akat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.akat.Main;
import net.akat.crystal.CrystalManager;
import net.akat.crystal.data.PlayerCrystalData;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CrystalEntityPacketListener extends PacketListenerAbstract {

    private final CrystalManager crystalManager = CrystalManager.getInstance();

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(event);
            int entityID = spawnPacket.getEntityId();

            ItemDisplay display = null;
            int crystalIndex = -1;
            for (Map.Entry<Integer, ItemDisplay> entry : crystalManager.getCrystalEntities().entrySet()) {
                if (entry.getValue().getEntityId() == entityID) {
                    display = entry.getValue();
                    crystalIndex = entry.getKey();
                    break;
                }
            }

            if (display == null) {
                return;
            }

            Player player = event.getPlayer();
            if (player == null || !player.isOnline()) {
                return;
            }
            UUID playerUUID = player.getUniqueId();
            PlayerCrystalData data = crystalManager.getPlayerData(playerUUID);

            if (data == null) {
                return;
            }

            if (crystalIndex != -1 && data.isCollected(crystalIndex)) {
                Runnable task = () -> crystalManager.sendPacket(player, crystalManager.getPacketFactory().createDestroyPacket(entityID));

                if (Main.isFolia()) {
                    Bukkit.getRegionScheduler().runDelayed(Main.getInstance(), player.getLocation(), (ignored) -> task.run(), 10L);
                } else {
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), task, 10L);
                }
            }
        }
    }
}
