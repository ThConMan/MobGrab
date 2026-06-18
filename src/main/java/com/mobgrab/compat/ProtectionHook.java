package com.mobgrab.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionHook {
    boolean canBuild(Player player, Location location);

    /**
     * Whether the player may grab a mob at this location. Defaults to the build check;
     * WorldGuard overrides it to also honour the custom {@code mob-grab} region flag.
     */
    default boolean canGrab(Player player, Location location) {
        return canBuild(player, location);
    }

    String getName();
}
