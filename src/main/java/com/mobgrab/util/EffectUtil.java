package com.mobgrab.util;

import com.mobgrab.MobGrab;
import com.mobgrab.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class EffectUtil {

    private EffectUtil() {}

    public static void playPickupEffect(Player player, Location location) {
        ConfigManager config = MobGrab.getInstance().getConfigManager();
        player.playSound(location, config.getPickupSound(),
                config.getPickupSoundVolume(), config.getPickupSoundPitch());
        location.getWorld().spawnParticle(
                config.getPickupParticles(), location.clone().add(0, 0.5, 0),
                config.getPickupParticleCount(), 0.3, 0.3, 0.3, 0.05);
    }

    public static void playPlaceEffect(Player player, Location location) {
        ConfigManager config = MobGrab.getInstance().getConfigManager();
        player.playSound(location, config.getPlaceSound(),
                config.getPlaceSoundVolume(), config.getPlaceSoundPitch());
        location.getWorld().spawnParticle(
                config.getPlaceParticles(), location.clone().add(0, 0.5, 0),
                config.getPlaceParticleCount(), 0.3, 0.3, 0.3, 0.05);
    }
}
