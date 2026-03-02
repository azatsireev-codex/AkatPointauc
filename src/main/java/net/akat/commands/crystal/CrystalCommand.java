package net.akat.commands.crystal;

import net.akat.crystal.CrystalManager;
import net.akat.crystal.CrystalPoint;
import net.akat.crystal.data.PlayerCrystalData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CrystalCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystal.admin")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eИспользование: /crystal <add|remove|list|reload>");
            return true;
        }

        CrystalManager manager = CrystalManager.getInstance();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cТолько игрок может добавлять кристаллы.");
                    return true;
                }
                Location loc = player.getLocation();
                World world = loc.getWorld();
                if (world == null) {
                    sender.sendMessage("§cНе удалось определить мир.");
                    return true;
                }

                CrystalPoint point = new CrystalPoint(
                        (byte) Bukkit.getWorlds().indexOf(world),
                        (float) loc.getX(),
                        (float) loc.getY(),
                        (float) loc.getZ()
                );

                manager.addCrystal(point);
                sender.sendMessage("§aКристалл добавлен!");
            }

            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /crystal remove <index>");
                    return true;
                }

                try {
                    int index = Integer.parseInt(args[1]);
                    List<CrystalPoint> crystals = manager.getStorage().getAll();

                    if (index < 0 || index >= crystals.size()) {
                        sender.sendMessage("§cКристалл с таким индексом не найден.");
                        return true;
                    }

                    boolean removed = manager.removeCrystal(index);

                    if (removed) {
                        sender.sendMessage("§aКристалл #" + index + " удалён.");
                    } else {
                        sender.sendMessage("§cНе удалось удалить кристалл.");
                    }

                } catch (NumberFormatException e) {
                    sender.sendMessage("§cИндекс должен быть числом.");
                }
            }

            case "list" -> {
                List<CrystalPoint> crystals = manager.getStorage().getAll();
                sender.sendMessage("§eКристаллы:");
                for (int i = 0; i < crystals.size(); i++) {
                    CrystalPoint p = crystals.get(i);
                    sender.sendMessage("§6[" + i + "] §7WorldID:" + p.worldId() + " X:" + p.x() + " Y:" + p.y() + " Z:" + p.z());
                }
            }
            case "resetcooldown" -> {
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /crystal resetcooldown <игрок>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) {
                    sender.sendMessage("§cИгрок не найден.");
                    return true;
                }

                PlayerCrystalData data = manager.getPlayerData(target.getUniqueId());
                if (data == null) {
                    sender.sendMessage("§cНет данных о кристаллах для этого игрока.");
                    return true;
                }

                data.getCollected().clear();
                data.setLastResetTime(System.currentTimeMillis());
                manager.getPlayerRepo().save(target.getUniqueId(), data);

                sender.sendMessage("§aКулдаун кристаллов для §e" + target.getName() + " §aуспешно сброшен!");
                if (target.isOnline()) {
                    ((Player) target).sendMessage("§d✨ Ваш кулдаун кристаллов был сброшен администратором!");
                }
            }

            default -> sender.sendMessage("§eИспользование: /crystal <add|remove|list|resetcooldown>");
        }

        return true;
    }
}
