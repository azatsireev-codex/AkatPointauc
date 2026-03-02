package net.akat.crystal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record CrystalPoint(byte worldId, float x, float y, float z) {

    public Location toLocation() {
        World world = Bukkit.getWorlds().get(worldId);
        return new Location(world, x, y, z);
    }
}