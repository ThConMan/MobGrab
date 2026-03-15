package com.mobgrab.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ProtectionManager {

    private final List<ProtectionHook> hooks = new ArrayList<>();

    public ProtectionManager(JavaPlugin plugin) {
        Logger log = plugin.getLogger();

        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                hooks.add(new WorldGuardHook());
                log.info("WorldGuard region protection enabled.");
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.warning("WorldGuard found but failed to hook: " + e.getMessage());
            }
        }

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlotSquared")) {
            try {
                Class.forName("com.plotsquared.core.PlotSquared");
                hooks.add(new PlotSquaredHook());
                log.info("PlotSquared region protection enabled.");
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.warning("PlotSquared found but failed to hook: " + e.getMessage());
            }
        }

        if (plugin.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
            try {
                Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                hooks.add(new GriefPreventionHook());
                log.info("GriefPrevention region protection enabled.");
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.warning("GriefPrevention found but failed to hook: " + e.getMessage());
            }
        }
    }

    public boolean canBuild(Player player, Location location) {
        for (ProtectionHook hook : hooks) {
            if (!hook.canBuild(player, location)) {
                return false;
            }
        }
        return true;
    }
}
