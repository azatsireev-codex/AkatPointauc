package net.akat;

import com.github.retrooper.packetevents.PacketEvents;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.akat.api.AkatPointaucAPI;
import net.akat.commands.*;
import net.akat.commands.completers.BidTabCompleter;
import net.akat.commands.crystal.CrystalCommand;
import net.akat.commands.crystal.CrystalTabCompleter;
import net.akat.crystal.CrystalManager;
import net.akat.crystal.animation.CrystalScheduler;
import net.akat.crystal.reward.ConfigBasedRewardStrategy;
import net.akat.crystal.reward.CrystalRewardStrategy;
import net.akat.listeners.CrystalClickListener;
import net.akat.listeners.CrystalEntityPacketListener;
import net.akat.listeners.PlayerListener;
import net.akat.managers.BalanceManager;
import net.akat.managers.BidManager;
import net.akat.managers.GiftManager;
import net.akat.placeholders.BalancePlaceholder;
import net.akat.task.BidCheckerTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin {

    public static Main instance;
    public BalanceManager balanceManager;
    public Database database;
    public GiftManager giftManager;
    public BidManager bidManager;
    public String pointaucToken;
    public CrystalManager crystalManager;
    public CrystalRewardStrategy crystalRewardStrategy;

    private AkatPointaucAPI api;

    private List<Integer> allowedBidAmounts;
    private boolean afkFarmEnabled;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadBidAmounts();
        loadPointaucToken();
        loadCrystalRewardStrategy();
        loadAfkFarmEnabled();

        this.database = new Database();
        this.database.connect();
        this.database.createTables();

        this.balanceManager = new BalanceManager();
        this.bidManager = new BidManager();
        this.giftManager = new GiftManager();

        api = new AkatPointaucAPI(balanceManager);

        try {
            CrystalManager.init(getDataFolder(), new File(getDataFolder(), "crystals.dat"));
            this.crystalManager = CrystalManager.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Не удалось загрузить кристаллы!");
        }

        if (isFolia()) {
            GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            scheduler.runAtFixedRate(this, task -> new BidCheckerTask(bidManager, balanceManager).run(), 100L, 100L);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new BidCheckerTask(bidManager, balanceManager), 100L, 100L);
        }

        startBalanceAutoSave();

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CrystalClickListener(), this);

        PacketEvents.getAPI().getEventManager().registerListener(new CrystalEntityPacketListener());

        getCommand("bid").setExecutor(new BidCommand());
        getCommand("bidamount").setExecutor(new BidAmountCommand());
        getCommand("toggleauction").setExecutor(new ToggleAuctionCommand(database));
        getCommand("reloadpointauc").setExecutor(new ReloadPointaucCommand());
        getCommand("giftclaim").setExecutor(new GiftClaimCommand(giftManager));
        getCommand("crystal").setExecutor(new CrystalCommand());

        getCommand("bid").setTabCompleter(new BidTabCompleter());
        getCommand("crystal").setTabCompleter(new CrystalTabCompleter());

        Bukkit.getServicesManager().register(AkatPointaucAPI.class, api, this, ServicePriority.Normal);

        giftManager.start();

        CrystalScheduler.start(this);

        new BalancePlaceholder().register();
    }

    @Override
    public void onDisable() {
        flushAllBalances();
        database.disconnect();
    }

    private void startBalanceAutoSave() {
        long periodTicks = getConfig().getLong("balance-autosave-interval-ticks", 600L);
        if (periodTicks <= 0) {
            getLogger().warning("Автосохранение баланса отключено (balance-autosave-interval-ticks <= 0)");
            return;
        }

        if (isFolia()) {
            GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            scheduler.runAtFixedRate(this, task -> flushDirtyBalances(), periodTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::flushDirtyBalances, periodTicks, periodTicks);
        }
    }

    public void flushDirtyBalances() {
        for (UUID uuid : balanceManager.consumeDirtyBalances()) {
            database.save(uuid, balanceManager.getPoints(uuid));
        }
    }

    public void flushAllBalances() {
        for (var entry : balanceManager.balances.entrySet()) {
            database.save(entry.getKey(), entry.getValue());
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void loadPointaucToken() {
        pointaucToken = getConfig().getString("pointauc-token");
    }

    private void loadCrystalRewardStrategy() {
        int min = getConfig().getInt("crystal-reward.min", 3);
        int max = getConfig().getInt("crystal-reward.max", 7);
        this.crystalRewardStrategy = new ConfigBasedRewardStrategy(min, max);
    }

    private void loadAfkFarmEnabled() {
        this.afkFarmEnabled = getConfig().getBoolean("afk-farm-enabled", true);
    }

    public boolean isAfkFarmEnabled() {
        return afkFarmEnabled;
    }

    public CrystalRewardStrategy getCrystalRewardStrategy() {
        return crystalRewardStrategy;
    }

    public void reloadPointaucConfig() {
        reloadConfig();
        loadPointaucToken();
        reloadBidAmounts();
        loadCrystalRewardStrategy();
        loadAfkFarmEnabled();
    }

    public void reloadBidAmounts() {
        this.allowedBidAmounts = getConfig().getIntegerList("allowed-bid-amounts");
    }

    public List<Integer> getAllowedBidAmounts() {
        return allowedBidAmounts;
    }

    public AkatPointaucAPI getAPI() {
        return api;
    }

    public static Main getInstance() {
        return instance;
    }
}
