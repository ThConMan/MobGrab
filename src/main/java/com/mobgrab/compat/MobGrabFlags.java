package com.mobgrab.compat;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.logging.Logger;

/**
 * The "mob-grab" WorldGuard flag. Set it to deny in a region to block pickups there.
 * Must be registered from onLoad(); null if WorldGuard isn't present.
 */
public final class MobGrabFlags {

    public static StateFlag MOB_GRAB = null;

    private MobGrabFlags() {
    }

    public static void register(Logger log) {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("mob-grab", true);
            try {
                registry.register(flag);
                MOB_GRAB = flag;
                log.info("Registered WorldGuard flag 'mob-grab'.");
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get("mob-grab");
                if (existing instanceof StateFlag sf) {
                    MOB_GRAB = sf;
                    log.info("Reusing existing WorldGuard flag 'mob-grab'.");
                } else {
                    log.warning("WorldGuard flag 'mob-grab' conflicts with an incompatible flag; grab-flag disabled.");
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            // WorldGuard not present (or its API moved) — feature simply unavailable.
            MOB_GRAB = null;
        }
    }
}
