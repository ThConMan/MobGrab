package com.mobgrab.compat;

import com.mobgrab.MobGrab;
import com.mobgrab.util.MobDataUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class GeyserSupport {

    private final MobGrab plugin;
    private boolean available;

    public GeyserSupport(MobGrab plugin) {
        this.plugin = plugin;
        if (!plugin.getConfigManager().isGeyserEnabled()) {
            available = false;
            return;
        }
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            available = plugin.getServer().getPluginManager().isPluginEnabled("floodgate");
            if (available) {
                plugin.getLogger().info("Floodgate detected - Bedrock form GUI enabled.");
            }
        } catch (ClassNotFoundException e) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isBedrockPlayer(Player player) {
        if (!available) return false;
        try {
            return FloodgateHelper.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception | NoClassDefFoundError e) {
            return false;
        }
    }

    public void openToggleForm(Player player, int page) {
        if (!available) return;
        try {
            List<EntityType> mobTypes = plugin.getMobToggleGUI().getMobTypes();
            int perPage = plugin.getConfigManager().getGeyserMobsPerPage();
            int totalPages = Math.max(1, (int) Math.ceil((double) mobTypes.size() / perPage));
            page = Math.max(0, Math.min(page, totalPages - 1));

            int start = page * perPage;
            int end = Math.min(start + perPage, mobTypes.size());

            FloodgateHelper.sendToggleForm(plugin, player, mobTypes, start, end, page, totalPages, perPage);
        } catch (Exception | NoClassDefFoundError e) {
            plugin.getLogger().warning("Failed to send Bedrock form to " + player.getName() + ": " + e.getMessage());
        }
    }
}
