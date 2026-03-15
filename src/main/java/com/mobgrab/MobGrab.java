package com.mobgrab;

import com.mobgrab.command.MobGrabCommand;
import com.mobgrab.compat.GeyserSupport;
import com.mobgrab.compat.ProtectionManager;
import com.mobgrab.config.ConfigManager;
import com.mobgrab.gui.MobToggleGUI;
import com.mobgrab.listener.PickupListener;
import com.mobgrab.listener.PlaceListener;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobGrab extends JavaPlugin {

    private static MobGrab instance;

    public static final NamespacedKey MOB_DATA_KEY = new NamespacedKey("mobgrab", "mob_data");
    public static final NamespacedKey MOB_TYPE_KEY = new NamespacedKey("mobgrab", "mob_type");
    public static final NamespacedKey MOB_STACK_KEY = new NamespacedKey("mobgrab", "mob_stack");

    public static final NamespacedKey CV_VILLAGER_KEY = new NamespacedKey("clickvillagers", "villager");
    public static final NamespacedKey CV_TYPE_KEY = new NamespacedKey("clickvillagers", "type");
    public static final NamespacedKey CV_NBT_KEY = new NamespacedKey("clickvillagers", "nbt");

    private ConfigManager configManager;
    private MobToggleGUI mobToggleGUI;
    private GeyserSupport geyserSupport;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        mobToggleGUI = new MobToggleGUI(this);
        geyserSupport = new GeyserSupport(this);
        protectionManager = new ProtectionManager(this);

        getServer().getPluginManager().registerEvents(new PickupListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceListener(this), this);
        getServer().getPluginManager().registerEvents(mobToggleGUI, this);

        var cmd = getCommand("mobgrab");
        if (cmd != null) {
            var handler = new MobGrabCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        getLogger().info("MobGrab v" + getDescription().getVersion() + " enabled"
                + (geyserSupport.isAvailable() ? " (Geyser support active)" : "") + ".");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static MobGrab getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MobToggleGUI getMobToggleGUI() {
        return mobToggleGUI;
    }

    public GeyserSupport getGeyserSupport() {
        return geyserSupport;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
