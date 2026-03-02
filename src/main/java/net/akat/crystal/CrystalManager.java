package net.akat.crystal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import net.akat.Main;
import net.akat.crystal.animation.CrystalAnimator;
import net.akat.crystal.data.CrystalStorage;
import net.akat.crystal.data.PlayerCrystalData;
import net.akat.crystal.data.PlayerCrystalRepository;
import net.akat.crystal.packet.CrystalPacketFactory;
import net.akat.crystal.reward.CrystalRewardStrategy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class CrystalManager {

    private static CrystalManager instance;
    public static CrystalManager getInstance() { return instance; }

    private final CrystalStorage storage;
    private final PlayerCrystalRepository playerRepo;
    private final CrystalAnimator animator;
    private final Map<UUID, PlayerCrystalData> playerData = new HashMap<>();
    private final Map<Integer, ItemDisplay> crystalEntities = new HashMap<>();

    private final Map<Integer, Long> spawnTimestamps = new HashMap<>();

    private final CrystalPacketFactory packetFactory = new CrystalPacketFactory();
    private final File crystalFile;

    private CrystalManager(File dataFolder, File crystalFile) throws IOException {
        this.crystalFile = crystalFile;

        if (!crystalFile.getParentFile().exists()) {
            crystalFile.getParentFile().mkdirs();
        }

        if (!crystalFile.exists()) {
            crystalFile.createNewFile();
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(crystalFile))) {
                out.writeShort(0);
            }
        }

        this.storage = new CrystalStorage();
        this.storage.load(crystalFile);

        this.playerRepo = new PlayerCrystalRepository(new File(dataFolder, "userdata"));
        this.animator = new CrystalAnimator();

        loadExistingCrystalsFromWorld();
    }

    public static void init(File dataFolder, File crystalFile) throws IOException {
        instance = new CrystalManager(dataFolder, crystalFile);
    }

    private void loadExistingCrystalsFromWorld() {
        List<CrystalPoint> crystals = storage.getAll();
        crystalEntities.clear();
        spawnTimestamps.clear();

        for (int i = 0; i < crystals.size(); i++) {
            CrystalPoint point = crystals.get(i);
            World world = Bukkit.getWorlds().get(point.worldId());
            if (world == null) continue;

            Location loc = new Location(world, point.x(), point.y(), point.z());

            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load();
            }

            ItemDisplay found = world.getNearbyEntities(loc, 0.7, 0.7, 0.7).stream()
                    .filter(e -> e instanceof ItemDisplay)
                    .map(e -> (ItemDisplay) e)
                    .findFirst()
                    .orElse(null);

            if (found != null) {
                crystalEntities.put(i, found);
                animator.registerCrystal(found, i);
                spawnTimestamps.put(found.getEntityId(), 0L);
            }
        }
    }

    public CrystalStorage getStorage() {
        return storage;
    }

    public CrystalPacketFactory getPacketFactory() {
        return packetFactory;
    }

    public void saveCrystals() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(crystalFile))) {
            List<CrystalPoint> list = storage.getAll();
            out.writeShort(list.size());
            for (CrystalPoint p : list) {
                out.writeByte(p.worldId());
                out.writeFloat(p.x());
                out.writeFloat(p.y());
                out.writeFloat(p.z());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCrystal(CrystalPoint point) {
        storage.add(point);
        saveCrystals();

        int index = storage.getAll().indexOf(point);
        ItemDisplay display = spawnCrystal(point);
        crystalEntities.put(index, display);
    }

    public boolean removeCrystal(int index) {
        ItemDisplay display = crystalEntities.get(index);
        if (display != null) {
            display.remove();
        }

        boolean removed = storage.remove(index);
        if (removed) {
            saveCrystals();

            Map<Integer, ItemDisplay> newMap = new HashMap<>();
            List<CrystalPoint> crystals = storage.getAll();
            for (int i = 0; i < crystals.size(); i++) {
                ItemDisplay d = crystalEntities.get(i >= index ? i + 1 : i);
                if (d != null) newMap.put(i, d);
            }
            crystalEntities.clear();
            crystalEntities.putAll(newMap);
        }
        return removed;
    }

    public List<CrystalPoint> getCrystals() {
        return storage.getAll();
    }

    public Map<Integer, ItemDisplay> getCrystalEntities() {
        return crystalEntities;
    }

    public void onPlayerJoin(Player p) {
        UUID uuid = p.getUniqueId();
        PlayerCrystalData data = playerRepo.load(uuid);
        playerData.put(uuid, data);
    }

    public void onCrystalCollect(Player p, int index) {
        PlayerCrystalData data = playerData.get(p.getUniqueId());
        if (data == null || data.isCollected(index)) return;

        ItemDisplay display = crystalEntities.get(index);
        if (display != null) {
            Long spawnTime = spawnTimestamps.get(display.getEntityId());
            if (spawnTime != null) {
                if (System.currentTimeMillis() - spawnTime < 10_000) {
                    p.sendMessage("§eНельзя подбирать кристалл сразу после спавна!");
                    return;
                }
                spawnTimestamps.remove(display.getEntityId());
            }
        }

        data.markCollected(index);
        data.setLastResetTime(System.currentTimeMillis());
        playerRepo.save(p.getUniqueId(), data);

        if (display != null) {
            sendPacket(p, packetFactory.createDestroyPacket(display.getEntityId()));

            Location loc = display.getLocation();
            p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
        }

        CrystalRewardStrategy strategy = Main.getInstance().getCrystalRewardStrategy();
        int reward = strategy.getRewardAmount();
        Main.getInstance().balanceManager.addPoints(p.getUniqueId(), reward);

        p.sendActionBar(
                Component.text("+", TextColor.fromHexString("#b866ff"))
                        .append(Component.text(reward + " ", TextColor.fromHexString("#b866ff")))
                        .append(Component.text("кубислав!", TextColor.fromHexString("#e7ccff")))
        );
    }

    public void onTick() {
        animator.animateAll();
    }

    public PlayerCrystalData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    public PlayerCrystalRepository getPlayerRepo() {
        return playerRepo;
    }

    private ItemDisplay spawnCrystal(CrystalPoint point) {
        World world = Bukkit.getWorlds().get(point.worldId());
        if (world == null) return null;

        Location bukkitLoc = new Location(world, point.x(), point.y(), point.z());

        ItemDisplay display = world.spawn(bukkitLoc, ItemDisplay.class, d -> {
            d.setItemStack(getCrystalItem());

            Transformation base = d.getTransformation();
            Transformation elevated = new Transformation(
                    new Vector3f(base.getTranslation().x(), base.getTranslation().y() + 1.0f, base.getTranslation().z()),
                    base.getLeftRotation(),
                    base.getScale(),
                    base.getRightRotation()
            );
            d.setTransformation(elevated);

            d.setInterpolationDuration(5);

            d.setGlowColorOverride(Color.fromRGB(148, 0, 211));
            d.setBrightness(new Display.Brightness(15, 15));
        });

        int index = storage.getAll().indexOf(point);
        animator.registerCrystal(display, index);

        spawnTimestamps.put(display.getEntityId(), System.currentTimeMillis());

        return display;
    }

    private ItemStack getCrystalItem() {
        ItemStack crystalItem = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = crystalItem.getItemMeta();
        if (meta != null) {
            NamespacedKey key = NamespacedKey.fromString("minecraft:hub/crystal");
            if (key != null) {
                meta.setItemModel(key);
            }

            meta.addEnchant(Enchantment.FLAME, 1, true);
            crystalItem.setItemMeta(meta);
        }
        return crystalItem;
    }

    public void sendPacket(Player p, PacketWrapper<?> packet) {
        if (p == null || !p.isOnline()) return;
        PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
    }
}
