package com.mobgrab.compat;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class WorldGuardHook implements ProtectionHook {

    @Override
    public boolean canBuild(Player player, Location location) {
        var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        var weLoc = BukkitAdapter.adapt(location);
        var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return query.testState(weLoc, wgPlayer, Flags.BUILD);
    }

    @Override
    public boolean canGrab(Player player, Location location) {
        // Grabbing requires build rights AND the custom mob-grab flag not being denied.
        if (!canBuild(player, location)) return false;
        if (MobGrabFlags.MOB_GRAB == null) return true;
        var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        var weLoc = BukkitAdapter.adapt(location);
        var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return query.queryState(weLoc, wgPlayer, MobGrabFlags.MOB_GRAB) != StateFlag.State.DENY;
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }
}
