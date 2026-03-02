package net.akat.crystal.packet;

import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;

public class CrystalPacketFactory {

    /**
     * Пакет для скрытия сущности для игрока
     */
    public PacketWrapper<?> createDestroyPacket(int entityId) {
        return new WrapperPlayServerDestroyEntities(entityId);
    }

    /**
     * Пакет для анимации (смещение вверх/вниз)
     */
    public PacketWrapper<?> createAnimationPacket(int entityId, float offsetY, Location baseLoc) {
        return new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(baseLoc.getX(), baseLoc.getY() + offsetY, baseLoc.getZ()),
                baseLoc.getYaw(),
                baseLoc.getPitch(),
                false
        );
    }
}
