package com.mobgrab.preset;

import com.mobgrab.MobGrab;
import com.mobgrab.util.EntitySerializer;
import com.mobgrab.util.MobDataUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PresetManager {

    private final MobGrab plugin;
    private final File presetsFile;
    private YamlConfiguration presetsConfig;

    public PresetManager(MobGrab plugin) {
        this.plugin = plugin;
        this.presetsFile = new File(plugin.getDataFolder(), "presets.yml");
        load();
    }

    public void load() {
        if (!presetsFile.exists()) {
            plugin.saveResource("presets.yml", false);
        }
        presetsConfig = YamlConfiguration.loadConfiguration(presetsFile);
        plugin.getLogger().info("Loaded " + presetsConfig.getKeys(false).size() + " mob presets.");
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            presetsConfig.save(presetsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save presets: " + e.getMessage());
        }
    }

    public Set<String> getPresetNames() {
        return presetsConfig.getKeys(false);
    }

    public boolean hasPreset(String name) {
        return presetsConfig.contains(name);
    }

    /**
     * Creates a MobGrab item from a preset definition.
     * MUST be called on the main server thread (spawns temporary entities).
     */
    public ItemStack createPresetItem(String presetName) {
        ConfigurationSection section = presetsConfig.getConfigurationSection(presetName);
        if (section == null) return null;

        // If raw SNBT is stored, use that directly
        String snbt = section.getString("snbt");
        if (snbt != null) {
            return createItemFromSnbt(section, snbt);
        }

        // Otherwise build from structured config
        return buildFromConfig(section);
    }

    private ItemStack createItemFromSnbt(ConfigurationSection section, String snbt) {
        String typeName = section.getString("type", "VILLAGER");
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown entity type in preset: " + typeName);
            return null;
        }

        World world = Bukkit.getWorlds().get(0);
        Location tempLoc = world.getSpawnLocation().clone().add(0, 200, 0);

        try {
            Entity entity = EntitySerializer.deserialize(snbt, entityType, tempLoc);
            if (entity == null) return null;

            ItemStack item = MobDataUtil.createMobItem(entity);
            entity.remove();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create preset item from SNBT: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack buildFromConfig(ConfigurationSection section) {
        String typeName = section.getString("type", "VILLAGER");
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown entity type in preset: " + typeName);
            return null;
        }

        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) {
            plugin.getLogger().warning("Cannot spawn entity type: " + typeName);
            return null;
        }

        World world = Bukkit.getWorlds().get(0);
        Location tempLoc = world.getSpawnLocation().clone().add(0, 200, 0);

        try {
            Entity entity = world.spawn(tempLoc, entityClass, e -> {
                e.setSilent(true);
                e.setInvulnerable(true);
                if (e instanceof Mob mob) mob.setAware(false);

                // Custom name
                String name = section.getString("name");
                if (name != null) {
                    e.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
                    e.setCustomNameVisible(true);
                }

                // Baby
                if (section.getBoolean("baby", false) && e instanceof Ageable ageable) {
                    ageable.setBaby();
                }

                // Health
                if (section.contains("health") && e instanceof LivingEntity living) {
                    double health = section.getDouble("health");
                    living.setMaxHealth(health);
                    living.setHealth(health);
                }

                // Villager specifics
                if (e instanceof Villager villager) {
                    configureVillager(villager, section);
                }

                // Horse specifics
                if (e instanceof Horse horse) {
                    String color = section.getString("color");
                    if (color != null) {
                        try { horse.setColor(Horse.Color.valueOf(color.toUpperCase())); }
                        catch (IllegalArgumentException ignored) {}
                    }
                    String style = section.getString("style");
                    if (style != null) {
                        try { horse.setStyle(Horse.Style.valueOf(style.toUpperCase())); }
                        catch (IllegalArgumentException ignored) {}
                    }
                }

                // Sheep color
                if (e instanceof Sheep sheep) {
                    String color = section.getString("color");
                    if (color != null) {
                        try { sheep.setColor(DyeColor.valueOf(color.toUpperCase())); }
                        catch (IllegalArgumentException ignored) {}
                    }
                }

                // Cat type
                if (e instanceof Cat cat) {
                    String catType = section.getString("cat-type");
                    if (catType != null) {
                        try {
                            var regCat = Registry.CAT_VARIANT.get(NamespacedKey.minecraft(catType.toLowerCase()));
                            if (regCat != null) cat.setCatType(regCat);
                        } catch (Exception ignored) {}
                    }
                }

                // Creeper charged
                if (e instanceof Creeper creeper && section.getBoolean("charged", false)) {
                    creeper.setPowered(true);
                }

                // Slime / Magma Cube size
                if (e instanceof Slime slime && section.contains("size")) {
                    slime.setSize(section.getInt("size"));
                }

                // Equipment
                if (e instanceof LivingEntity living) {
                    configureEquipment(living, section);
                }
            });

            ItemStack item = MobDataUtil.createMobItem(entity);
            entity.remove();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to build preset: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void configureVillager(Villager villager, ConfigurationSection section) {
        // Profession
        String prof = section.getString("profession");
        if (prof != null) {
            try {
                villager.setProfession(Villager.Profession.valueOf(prof.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown villager profession: " + prof);
            }
        }

        // Level (1-5)
        villager.setVillagerLevel(section.getInt("level", 1));

        // Villager type (biome variant)
        String villagerType = section.getString("villager-type");
        if (villagerType != null) {
            try {
                villager.setVillagerType(Villager.Type.valueOf(villagerType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        // Trades
        List<Map<?, ?>> tradeList = section.getMapList("trades");
        if (!tradeList.isEmpty()) {
            List<MerchantRecipe> recipes = new ArrayList<>();
            for (Map<?, ?> tradeMap : tradeList) {
                MerchantRecipe recipe = buildTrade(tradeMap);
                if (recipe != null) {
                    recipes.add(recipe);
                }
            }
            if (!recipes.isEmpty()) {
                villager.setRecipes(recipes);
            }
        }
    }

    private MerchantRecipe buildTrade(Map<?, ?> tradeMap) {
        // Parse result item
        String resultStr = getString(tradeMap, "result");
        if (resultStr == null) return null;
        ItemStack result = parseItemStack(resultStr);
        if (result == null) return null;

        // Apply enchantments to result
        Object enchObj = tradeMap.get("enchantments");
        if (enchObj instanceof Map<?, ?> enchMap) {
            applyEnchantments(result, enchMap);
        }

        // Custom result name
        String resultName = getString(tradeMap, "result-name");
        if (resultName != null) {
            var meta = result.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(resultName));
            result.setItemMeta(meta);
        }

        // Max uses
        int maxUses = getInt(tradeMap, "max-uses", 9999);

        // Experience reward
        boolean giveExp = getBoolean(tradeMap, "give-exp", false);

        MerchantRecipe recipe = new MerchantRecipe(result, 0, maxUses, giveExp);

        // Parse first ingredient (required)
        String ing1Str = getString(tradeMap, "ingredient1");
        if (ing1Str == null) return null;
        ItemStack ing1 = parseItemStack(ing1Str);
        if (ing1 == null) return null;
        recipe.addIngredient(ing1);

        // Parse second ingredient (optional)
        String ing2Str = getString(tradeMap, "ingredient2");
        if (ing2Str != null) {
            ItemStack ing2 = parseItemStack(ing2Str);
            if (ing2 != null) {
                recipe.addIngredient(ing2);
            }
        }

        return recipe;
    }

    private void applyEnchantments(ItemStack item, Map<?, ?> enchMap) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            for (var entry : enchMap.entrySet()) {
                Enchantment ench = resolveEnchantment(entry.getKey().toString());
                if (ench != null) {
                    meta.addStoredEnchant(ench, ((Number) entry.getValue()).intValue(), true);
                }
            }
            item.setItemMeta(meta);
        } else {
            for (var entry : enchMap.entrySet()) {
                Enchantment ench = resolveEnchantment(entry.getKey().toString());
                if (ench != null) {
                    item.addUnsafeEnchantment(ench, ((Number) entry.getValue()).intValue());
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Enchantment resolveEnchantment(String name) {
        // Try registry lookup first (minecraft key)
        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase()));
        if (ench != null) return ench;

        // Fallback: try legacy name
        return Enchantment.getByName(name.toUpperCase());
    }

    private ItemStack parseItemStack(String str) {
        if (str == null || str.equalsIgnoreCase("null")) return null;
        String[] parts = str.split(":");
        Material mat;
        try {
            mat = Material.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + parts[0]);
            return null;
        }
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new ItemStack(mat, amount);
    }

    private void configureEquipment(LivingEntity living, ConfigurationSection section) {
        var equipment = living.getEquipment();
        if (equipment == null) return;

        ConfigurationSection eqSection = section.getConfigurationSection("equipment");
        if (eqSection == null) return;

        setEquipmentSlot(eqSection, "helmet", equipment::setHelmet);
        setEquipmentSlot(eqSection, "chestplate", equipment::setChestplate);
        setEquipmentSlot(eqSection, "leggings", equipment::setLeggings);
        setEquipmentSlot(eqSection, "boots", equipment::setBoots);
        setEquipmentSlot(eqSection, "main-hand", equipment::setItemInMainHand);
        setEquipmentSlot(eqSection, "off-hand", equipment::setItemInOffHand);
    }

    private void setEquipmentSlot(ConfigurationSection eqSection, String slot,
                                   java.util.function.Consumer<ItemStack> setter) {
        ConfigurationSection slotSection = eqSection.getConfigurationSection(slot);
        if (slotSection == null) {
            // Simple format: just item string
            String itemStr = eqSection.getString(slot);
            if (itemStr != null) {
                ItemStack item = parseItemStack(itemStr);
                if (item != null) setter.accept(item);
            }
            return;
        }

        // Complex format with enchantments
        String itemStr = slotSection.getString("item");
        if (itemStr == null) return;
        ItemStack item = parseItemStack(itemStr);
        if (item == null) return;

        ConfigurationSection enchSection = slotSection.getConfigurationSection("enchantments");
        if (enchSection != null) {
            Map<String, Object> enchMap = new HashMap<>();
            for (String key : enchSection.getKeys(false)) {
                enchMap.put(key, enchSection.getInt(key));
            }
            applyEnchantments(item, enchMap);
        }

        setter.accept(item);
    }

    /**
     * Save a live entity as a preset using SNBT serialization.
     */
    public void savePreset(String name, Entity entity) {
        String snbt = EntitySerializer.serialize(entity);
        presetsConfig.set(name + ".type", entity.getType().name());
        presetsConfig.set(name + ".snbt", snbt);
        save();
        plugin.getLogger().info("Saved preset: " + name + " (" + entity.getType().name() + ")");
    }

    /**
     * Delete a preset by name.
     */
    public boolean deletePreset(String name) {
        if (!presetsConfig.contains(name)) return false;
        presetsConfig.set(name, null);
        save();
        return true;
    }

    // --- Utility methods for parsing untyped maps ---

    private static String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static int getInt(Map<?, ?> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number num) return num.intValue();
        return defaultVal;
    }

    private static boolean getBoolean(Map<?, ?> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean bool) return bool;
        return defaultVal;
    }
}
