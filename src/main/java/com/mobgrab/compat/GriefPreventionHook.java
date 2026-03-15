package com.mobgrab.compat;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class GriefPreventionHook implements ProtectionHook {

    @Override
    public boolean canBuild(Player player, Location location) {
        String denial = GriefPrevention.instance.allowBuild(player, location);
        return denial == null;
    }

    @Override
    public String getName() {
        return "GriefPrevention";
    }
}
