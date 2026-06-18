package com.mobgrab.config;

import com.mobgrab.MobGrab;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

public final class ConfigManager {

    private final MobGrab plugin;
    private final Map<EntityType, Boolean> enabledMobs = new EnumMap<>(EntityType.class);

    private Sound pickupSound;
    private Sound placeSound;
    private Particle pickupParticles;
    private Particle placeParticles;
    private int pickupParticleCount;
    private int placeParticleCount;
    private float pickupSoundVolume;
    private float pickupSoundPitch;
    private float placeSoundVolume;
    private float placeSoundPitch;
    private int cooldownSeconds;
    private boolean blacklistMode;
    private boolean geyserEnabled;
    private int geyserMobsPerPage;
    private boolean roseStackerPickupWholeStack;
    private boolean fireproofItems;
    private final java.util.Set<String> disabledWorlds = new java.util.HashSet<>();

    public ConfigManager(MobGrab plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        pickupSound = parseSound(config.getString("pickup-sound"), Sound.ENTITY_CHICKEN_EGG);
        placeSound = parseSound(config.getString("place-sound"), Sound.ENTITY_ENDERMAN_TELEPORT);
        pickupParticles = parseParticle(config.getString("pickup-particles"), Particle.SMOKE);
        placeParticles = parseParticle(config.getString("place-particles"), Particle.HAPPY_VILLAGER);
        pickupParticleCount = config.getInt("pickup-particle-count", 15);
        placeParticleCount = config.getInt("place-particle-count", 20);
        pickupSoundVolume = (float) config.getDouble("pickup-sound-volume", 0.8);
        pickupSoundPitch = (float) config.getDouble("pickup-sound-pitch", 1.4);
        placeSoundVolume = (float) config.getDouble("place-sound-volume", 0.6);
        placeSoundPitch = (float) config.getDouble("place-sound-pitch", 1.2);
        cooldownSeconds = config.getInt("cooldown-seconds", 1);
        blacklistMode = config.getBoolean("blacklist-mode", false);
        geyserEnabled = config.getBoolean("geyser.enabled", true);
        geyserMobsPerPage = config.getInt("geyser.mobs-per-page", 20);
        roseStackerPickupWholeStack = config.getBoolean("rosestacker-pickup-whole-stack", false);
        fireproofItems = config.getBoolean("fireproof-items", true);
        disabledWorlds.clear();
        for (String w : config.getStringList("disabled-worlds")) {
            disabledWorlds.add(w.toLowerCase());
        }

        enabledMobs.clear();
        var section = config.getConfigurationSection("enabled-mobs");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    enabledMobs.put(EntityType.valueOf(key.toUpperCase()), section.getBoolean(key));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Unknown entity type in config: " + key);
                }
            }
            plugin.getLogger().info("Loaded " + enabledMobs.size() + " mob toggles.");
        } else {
            plugin.getLogger().warning("No 'enabled-mobs' section found in config!");
        }
    }

    public boolean isMobEnabled(EntityType type) {
        Boolean enabled = enabledMobs.get(type);
        return enabled != null ? enabled : blacklistMode;
    }

    public void toggleMob(EntityType type) {
        boolean newState = !isMobEnabled(type);
        enabledMobs.put(type, newState);
        plugin.getConfig().set("enabled-mobs." + type.name(), newState);
        plugin.saveConfig();
    }

    public Map<EntityType, Boolean> getEnabledMobs() {
        return enabledMobs;
    }

    public Sound getPickupSound()          { return pickupSound; }
    public Sound getPlaceSound()           { return placeSound; }
    public Particle getPickupParticles()   { return pickupParticles; }
    public Particle getPlaceParticles()    { return placeParticles; }
    public int getPickupParticleCount()    { return pickupParticleCount; }
    public int getPlaceParticleCount()     { return placeParticleCount; }
    public float getPickupSoundVolume()    { return pickupSoundVolume; }
    public float getPickupSoundPitch()     { return pickupSoundPitch; }
    public float getPlaceSoundVolume()     { return placeSoundVolume; }
    public float getPlaceSoundPitch()      { return placeSoundPitch; }
    public int getCooldownSeconds()        { return cooldownSeconds; }
    public boolean isGeyserEnabled()       { return geyserEnabled; }
    public int getGeyserMobsPerPage()      { return geyserMobsPerPage; }
    public boolean isRoseStackerPickupWholeStack() { return roseStackerPickupWholeStack; }
    public boolean isFireproofItems()      { return fireproofItems; }

    public boolean isWorldDisabled(org.bukkit.World world) {
        return world != null && disabledWorlds.contains(world.getName().toLowerCase());
    }

    /** Toggle fireproof mob items at runtime (e.g. from the admin GUI) and persist it. */
    public void setFireproofItems(boolean value) {
        this.fireproofItems = value;
        plugin.getConfig().set("fireproof-items", value);
        plugin.saveConfig();
    }

    private static Sound parseSound(String value, Sound fallback) {
        if (value == null || value.isBlank()) return fallback;
        // Sound#valueOf was removed/marked-for-removal once sounds became registry-backed.
        // Look up via Registry.SOUNDS. Accept legacy enum-style names (ENTITY_CHICKEN_EGG),
        // dotted keys (entity.chicken.egg) and namespaced keys (minecraft:entity.chicken.egg).
        String v = value.toLowerCase();
        String path = v.contains(":") ? v.substring(v.indexOf(':') + 1) : v;
        if (!path.contains(".")) path = path.replace('_', '.');
        String namespace = v.contains(":") ? v.substring(0, v.indexOf(':')) : "minecraft";
        NamespacedKey key = NamespacedKey.fromString(namespace + ":" + path);
        if (key == null) return fallback;
        Sound sound = Registry.SOUNDS.get(key);
        return sound != null ? sound : fallback;
    }

    private static Particle parseParticle(String value, Particle fallback) {
        if (value == null) return fallback;
        try {
            return Particle.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
