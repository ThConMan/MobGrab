package com.mobgrab.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionHook {
    boolean canBuild(Player player, Location location);
    String getName();
}
