package com.mobgrab.util;

import com.mobgrab.MobGrab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MobDataUtil {

    private MobDataUtil() {}

    public static ItemStack createMobItem(Entity entity) {
        return createMobItem(entity, 1);
    }

    public static ItemStack createMobItem(Entity entity, int stackSize) {
        EntityType type = entity.getType();
        ItemStack item;
        if (entity instanceof Villager v) {
            item = HeadUtil.getVillagerHead(v.getProfession());
        } else {
            item = HeadUtil.getMobHead(type);
        }
        ItemMeta meta = item.getItemMeta();

        Component customName = entity.customName();
        String baseName = customName != null
                ? PlainTextComponentSerializer.plainText().serialize(customName)
                : formatEntityName(type);

        Component title;
        if (stackSize > 1) {
            title = Component.text(stackSize, NamedTextColor.YELLOW)
                    .append(Component.text("\u00D7 ", NamedTextColor.GRAY))
                    .append(Component.text(baseName, NamedTextColor.GOLD));
        } else {
            title = Component.text(baseName, NamedTextColor.GOLD);
        }
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (stackSize > 1) {
            lore.add(Component.text("\u2630 ", NamedTextColor.YELLOW)
                    .append(Component.text("Stacked Mob ", NamedTextColor.GRAY))
                    .append(Component.text("(" + stackSize + ")", NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        lore.add(line(formatEntityName(type), NamedTextColor.GRAY));

        if (customName != null) {
            String name = PlainTextComponentSerializer.plainText().serialize(customName);
            lore.add(label("Name", name, NamedTextColor.AQUA));
        }

        if (entity instanceof LivingEntity living) {
            lore.add(label("Health",
                    String.format("%.1f/%.1f", living.getHealth(), living.getMaxHealth()),
                    NamedTextColor.RED));
        }

        if (entity instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
            lore.add(label("Owner", tameable.getOwner().getName(), NamedTextColor.GREEN));
        }

        // Villager
        if (entity instanceof Villager v) {
            String prof = formatEnum(v.getProfession().getKey().getKey());
            if (!prof.equals("None")) {
                lore.add(label("Profession", prof, NamedTextColor.YELLOW));
            }
            lore.add(label("Level", String.valueOf(v.getVillagerLevel()), NamedTextColor.YELLOW));
            List<MerchantRecipe> recipes = v.getRecipes();
            if (!recipes.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Trades (" + recipes.size() + ")", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                for (MerchantRecipe recipe : recipes) {
                    addTradeLine(lore, recipe);
                }
            }
        }

        // Horse
        if (entity instanceof Horse horse) {
            lore.add(label("Color", formatEnum(horse.getColor().name()), NamedTextColor.WHITE));
            lore.add(label("Style", formatEnum(horse.getStyle().name()), NamedTextColor.WHITE));
        }

        // Cat
        if (entity instanceof Cat cat) {
            lore.add(label("Type", formatEnum(cat.getCatType().getKey().getKey()), NamedTextColor.WHITE));
            if (cat.isTamed() && cat.getCollarColor() != null) {
                lore.add(label("Collar", formatEnum(cat.getCollarColor().name()), NamedTextColor.WHITE));
            }
        }

        // Wolf
        if (entity instanceof Wolf wolf) {
            if (wolf.isTamed() && wolf.getCollarColor() != null) {
                lore.add(label("Collar", formatEnum(wolf.getCollarColor().name()), NamedTextColor.WHITE));
            }
        }

        // Sheep
        if (entity instanceof Sheep sheep && sheep.getColor() != null) {
            lore.add(label("Color", formatEnum(sheep.getColor().name()), NamedTextColor.WHITE));
            if (sheep.isSheared()) {
                lore.add(tag("Sheared", NamedTextColor.GRAY));
            }
        }

        // Axolotl
        if (entity instanceof Axolotl axolotl) {
            lore.add(label("Variant", formatEnum(axolotl.getVariant().name()), NamedTextColor.LIGHT_PURPLE));
        }

        // Frog
        if (entity instanceof Frog frog) {
            lore.add(label("Variant", formatEnum(frog.getVariant().getKey().getKey()), NamedTextColor.WHITE));
        }

        // Parrot
        if (entity instanceof Parrot parrot) {
            lore.add(label("Color", formatEnum(parrot.getVariant().name()), NamedTextColor.WHITE));
        }

        // Fox
        if (entity instanceof Fox fox) {
            lore.add(label("Type", formatEnum(fox.getFoxType().name()), NamedTextColor.WHITE));
        }

        // Rabbit
        if (entity instanceof Rabbit rabbit) {
            lore.add(label("Type", formatEnum(rabbit.getRabbitType().name()), NamedTextColor.WHITE));
        }

        // Mooshroom
        if (entity instanceof MushroomCow mooshroom) {
            lore.add(label("Variant", formatEnum(mooshroom.getVariant().name()), NamedTextColor.WHITE));
        }

        // Llama
        if (entity instanceof Llama llama) {
            lore.add(label("Color", formatEnum(llama.getColor().name()), NamedTextColor.WHITE));
            lore.add(label("Strength", String.valueOf(llama.getStrength()), NamedTextColor.WHITE));
        }

        // Panda
        if (entity instanceof Panda panda) {
            lore.add(label("Personality", formatEnum(panda.getMainGene().name()), NamedTextColor.WHITE));
        }

        // Bee
        if (entity instanceof Bee bee) {
            if (bee.hasNectar()) {
                lore.add(tag("Has Nectar", NamedTextColor.YELLOW));
            }
            if (bee.hasStung()) {
                lore.add(tag("Has Stung", NamedTextColor.RED));
            }
            if (bee.getAnger() > 0) {
                lore.add(tag("Angry", NamedTextColor.RED));
            }
        }

        // Goat
        if (entity instanceof Goat goat) {
            if (goat.isScreaming()) {
                lore.add(tag("Screaming", NamedTextColor.RED));
            }
            if (!goat.hasLeftHorn() || !goat.hasRightHorn()) {
                String horns = goat.hasLeftHorn() && goat.hasRightHorn() ? "Both"
                        : goat.hasLeftHorn() ? "Left Only"
                        : goat.hasRightHorn() ? "Right Only"
                        : "None";
                lore.add(label("Horns", horns, NamedTextColor.WHITE));
            }
        }

        // Creeper
        if (entity instanceof Creeper creeper) {
            if (creeper.isPowered()) {
                lore.add(tag("Charged", NamedTextColor.LIGHT_PURPLE));
            }
        }

        // Slime / Magma Cube
        if (entity instanceof Slime slime) {
            String sizeName = switch (slime.getSize()) {
                case 1 -> "Tiny";
                case 2 -> "Small";
                case 4 -> "Big";
                default -> "Size " + slime.getSize();
            };
            lore.add(label("Size", sizeName, NamedTextColor.WHITE));
        }

        // Phantom
        if (entity instanceof Phantom phantom) {
            String sizeName = switch (phantom.getSize()) {
                case 0 -> "Small";
                case 1 -> "Medium";
                case 2 -> "Large";
                default -> "Size " + phantom.getSize();
            };
            lore.add(label("Size", sizeName, NamedTextColor.WHITE));
        }

        // Tropical Fish
        if (entity instanceof TropicalFish fish) {
            lore.add(label("Pattern", formatEnum(fish.getPattern().name()), NamedTextColor.WHITE));
            lore.add(label("Body", formatEnum(fish.getBodyColor().name()), NamedTextColor.WHITE));
            lore.add(label("Pattern Color", formatEnum(fish.getPatternColor().name()), NamedTextColor.WHITE));
        }

        // PufferFish
        if (entity instanceof PufferFish puffer) {
            String state = switch (puffer.getPuffState()) {
                case 0 -> "Deflated";
                case 1 -> "Half Puffed";
                case 2 -> "Fully Puffed";
                default -> "State " + puffer.getPuffState();
            };
            lore.add(label("Puff", state, NamedTextColor.WHITE));
        }

        // Strider
        if (entity instanceof Strider strider) {
            if (strider.isShivering()) {
                lore.add(tag("Shivering", NamedTextColor.AQUA));
            }
        }

        // Copper Golem
        if (entity instanceof CopperGolem copperGolem) {
            lore.add(label("Weathering", formatEnum(copperGolem.getWeatheringState().name()), NamedTextColor.GOLD));
        }

        // Creaking
        if (entity instanceof Creaking creaking) {
            if (creaking.isActive()) {
                lore.add(tag("Active", NamedTextColor.RED));
            }
        }

        // Shulker
        if (entity instanceof Shulker shulker) {
            if (shulker.getColor() != null) {
                lore.add(label("Color", formatEnum(shulker.getColor().name()), NamedTextColor.WHITE));
            }
        }

        if (entity instanceof LivingEntity living && living.getEquipment() != null) {
            var eq = living.getEquipment();
            addEquipmentLine(lore, "Helmet", eq.getHelmet());
            addEquipmentLine(lore, "Chestplate", eq.getChestplate());
            addEquipmentLine(lore, "Leggings", eq.getLeggings());
            addEquipmentLine(lore, "Boots", eq.getBoots());
            addEquipmentLine(lore, "Holding", eq.getItemInMainHand());
            addEquipmentLine(lore, "Off Hand", eq.getItemInOffHand());
        }

        if (entity instanceof Ageable ageable && !ageable.isAdult()) {
            lore.add(tag("Baby", NamedTextColor.AQUA));
        }

        if (entity instanceof Sittable sittable && sittable.isSitting()) {
            lore.add(tag("Sitting", NamedTextColor.AQUA));
        }

        lore.add(Component.empty());
        if (stackSize > 1) {
            lore.add(line("Right-click to place stack", NamedTextColor.YELLOW));
        } else {
            lore.add(line("Right-click to place", NamedTextColor.YELLOW));
        }

        meta.lore(lore);

        String serialized = EntitySerializer.serialize(entity);
        meta.getPersistentDataContainer().set(MobGrab.MOB_DATA_KEY, PersistentDataType.STRING, serialized);
        meta.getPersistentDataContainer().set(MobGrab.MOB_TYPE_KEY, PersistentDataType.STRING, type.name());
        if (stackSize > 1) {
            meta.getPersistentDataContainer().set(MobGrab.MOB_STACK_KEY, PersistentDataType.INTEGER, stackSize);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMobItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(MobGrab.MOB_DATA_KEY, PersistentDataType.STRING);
    }

    public static String formatEntityName(EntityType type) {
        return formatEnum(type.name());
    }

    static String formatEnum(String name) {
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component tag(String text, NamedTextColor color) {
        return Component.text("\u25C6 ", NamedTextColor.DARK_GRAY)
                .append(Component.text(text, color).decoration(TextDecoration.BOLD, true))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static Component label(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.DARK_GRAY)
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static void addEquipmentLine(List<Component> lore, String slotName, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        String itemName = getItemDisplayName(item);
        lore.add(label(slotName, itemName, NamedTextColor.LIGHT_PURPLE));

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            for (var entry : enchants.entrySet()) {
                String enchName = formatEnum(entry.getKey().getKey().getKey());
                String level = toRoman(entry.getValue());
                lore.add(Component.text("  " + enchName + " " + level, NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
    }

    private static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return formatEnum(item.getType().name());
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    private static void addTradeLine(List<Component> lore, MerchantRecipe recipe) {
        List<ItemStack> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResult();

        Component ingredientComp = formatTradeItem(ingredients.get(0));
        if (ingredients.size() > 1 && !ingredients.get(1).getType().isAir()) {
            ingredientComp = ingredientComp
                    .append(Component.text(" + ", NamedTextColor.DARK_GRAY))
                    .append(formatTradeItem(ingredients.get(1)));
        }

        Component arrow = Component.text(" → ", NamedTextColor.DARK_GRAY);
        Component resultComp = formatTradeItem(result);

        Map<Enchantment, Integer> bookEnchants = getTradeEnchantments(result);
        if (!bookEnchants.isEmpty() && result.getType() == Material.ENCHANTED_BOOK) {
            for (var entry : bookEnchants.entrySet()) {
                String enchName = formatEnum(entry.getKey().getKey().getKey()) + " " + toRoman(entry.getValue());
                Component enchResult = Component.text("\uD83D\uDCD6 ", NamedTextColor.WHITE)
                        .append(Component.text(enchName, NamedTextColor.LIGHT_PURPLE));

                Component tradeLine = Component.text("  ", NamedTextColor.GRAY)
                        .append(ingredientComp)
                        .append(arrow)
                        .append(enchResult)
                        .decoration(TextDecoration.ITALIC, false);

                if (recipe.getUses() >= recipe.getMaxUses()) {
                    tradeLine = tradeLine.decoration(TextDecoration.STRIKETHROUGH, true)
                            .color(NamedTextColor.DARK_GRAY);
                }
                lore.add(tradeLine);
            }
        } else {
            Component tradeLine = Component.text("  ", NamedTextColor.GRAY)
                    .append(ingredientComp)
                    .append(arrow)
                    .append(resultComp)
                    .decoration(TextDecoration.ITALIC, false);

            if (recipe.getUses() >= recipe.getMaxUses()) {
                tradeLine = tradeLine.decoration(TextDecoration.STRIKETHROUGH, true)
                        .color(NamedTextColor.DARK_GRAY);
            }
            lore.add(tradeLine);
        }
    }

    private static Component formatTradeItem(ItemStack item) {
        String icon = getItemIcon(item.getType());
        String name = getTradeItemName(item);
        int amount = item.getAmount();
        String display = amount > 1 ? amount + "x " + name : name;

        return Component.text(icon + " ", NamedTextColor.WHITE)
                .append(Component.text(display, NamedTextColor.GRAY));
    }

    private static String getTradeItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return getShortName(item.getType());
    }

    private static Map<Enchantment, Integer> getTradeEnchantments(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK && item.hasItemMeta()
                && item.getItemMeta() instanceof EnchantmentStorageMeta esm) {
            return esm.getStoredEnchants();
        }
        if (!item.getEnchantments().isEmpty()) {
            return item.getEnchantments();
        }
        return Map.of();
    }

    private static String getItemIcon(Material mat) {
        return switch (mat) {
            // Currency
            case EMERALD -> "\uD83D\uDC8E"; // 💎 green
            case EMERALD_BLOCK -> "\uD83D\uDC8E";
            case DIAMOND -> "\uD83D\uDC8E";
            case LAPIS_LAZULI -> "\uD83D\uDC8E";

            // Books
            case ENCHANTED_BOOK -> "\uD83D\uDCD6"; // 📖
            case BOOK, WRITABLE_BOOK, WRITTEN_BOOK -> "\uD83D\uDCD6";
            case PAPER, MAP, FILLED_MAP -> "\uD83D\uDCC4"; // 📄

            // Food / Crops
            case WHEAT -> "\uD83C\uDF3E"; // 🌾
            case CARROT -> "\uD83E\uDD55"; // 🥕
            case POTATO, BAKED_POTATO -> "\uD83E\uDD54"; // 🥔
            case APPLE, GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE -> "\uD83C\uDF4E"; // 🍎
            case SWEET_BERRIES, GLOW_BERRIES -> "\uD83C\uDF47"; // 🍇
            case MELON_SLICE, MELON -> "\uD83C\uDF49"; // 🍉
            case PUMPKIN, CARVED_PUMPKIN -> "\uD83C\uDF83"; // 🎃
            case KELP -> "\uD83C\uDF3F"; // 🌿
            case BREAD -> "\uD83C\uDF5E"; // 🍞
            case COOKIE -> "\uD83C\uDF6A"; // 🍪
            case CAKE -> "\uD83C\uDF82"; // 🎂

            // Tools & Weapons
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> "\uD83D\uDDE1"; // 🗡
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE -> "⛏";
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> "\uD83E\uDE93"; // 🪓
            case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL -> "\uD83E\uDEA3"; // 🪣-ish
            case BOW, CROSSBOW -> "\uD83C\uDFF9"; // 🏹
            case ARROW, TIPPED_ARROW, SPECTRAL_ARROW -> "➵";
            case FISHING_ROD -> "\uD83C\uDFA3"; // 🎣
            case SHEARS -> "✂";
            case TRIDENT -> "\uD83D\uDD31"; // 🔱
            case SHIELD -> "\uD83D\uDEE1\uFE0F"; // 🛡️

            // Armor
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS,
                 CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS,
                 IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS,
                 GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS,
                 DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS,
                 NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> "\uD83D\uDC55"; // 👕

            // Animals
            case CHICKEN, COOKED_CHICKEN -> "\uD83D\uDC13"; // 🐓
            case PORKCHOP, COOKED_PORKCHOP -> "\uD83D\uDC37"; // 🐷
            case BEEF, COOKED_BEEF -> "\uD83D\uDC04"; // 🐄
            case RABBIT, COOKED_RABBIT, RABBIT_HIDE -> "\uD83D\uDC07"; // 🐇
            case MUTTON, COOKED_MUTTON -> "\uD83D\uDC11"; // 🐑
            case COD, COOKED_COD, SALMON, COOKED_SALMON, TROPICAL_FISH -> "\uD83D\uDC1F"; // 🐟
            case TURTLE_SCUTE -> "\uD83D\uDC22"; // 🐢
            case LEATHER, RABBIT_FOOT -> "\uD83D\uDC34"; // 🐴

            // Dyes & Wool
            case WHITE_WOOL, BLACK_WOOL, BLUE_WOOL, BROWN_WOOL, CYAN_WOOL, GRAY_WOOL,
                 GREEN_WOOL, LIGHT_BLUE_WOOL, LIGHT_GRAY_WOOL, LIME_WOOL, MAGENTA_WOOL,
                 ORANGE_WOOL, PINK_WOOL, PURPLE_WOOL, RED_WOOL, YELLOW_WOOL -> "\uD83E\uDDF6"; // 🧶

            // Potions & Brewing
            case POTION, SPLASH_POTION, LINGERING_POTION -> "\uD83E\uDDEA"; // 🧪
            case BLAZE_POWDER, BLAZE_ROD -> "\uD83D\uDD25"; // 🔥
            case NETHER_WART -> "\uD83C\uDF3F"; // 🌿

            // Blocks & Materials
            case STONE, COBBLESTONE, ANDESITE, DIORITE, GRANITE -> "\uD83E\uDEA8"; // 🪨
            case CLAY_BALL, CLAY, BRICK, BRICKS -> "\uD83E\uDDF1"; // 🧱
            case GLASS_PANE, GLASS -> "\uD83D\uDCCB"; // 📋
            case COAL, CHARCOAL -> "\u25C6"; // ◆
            case IRON_INGOT -> "\u2699"; // ⚙
            case GOLD_INGOT -> "\uD83C\uDF1F"; // 🌟
            case REDSTONE -> "◎";

            // Painting & Decoration
            case PAINTING -> "\uD83D\uDDBC"; // 🖼
            case ITEM_FRAME, GLOW_ITEM_FRAME -> "\uD83D\uDDBC";
            case FLOWER_BANNER_PATTERN, GLOBE_BANNER_PATTERN -> "\uD83C\uDFF3\uFE0F"; // 🏳️

            // Misc
            case ENDER_PEARL, ENDER_EYE -> "\uD83D\uDC41"; // 👁
            case EXPERIENCE_BOTTLE -> "✨";
            case NAME_TAG -> "\uD83C\uDFF7\uFE0F"; // 🏷️
            case SADDLE -> "\uD83D\uDC0E"; // 🐎
            case COMPASS, RECOVERY_COMPASS -> "\uD83E\uDDED"; // 🧭
            case CLOCK -> "\uD83D\uDD70\uFE0F"; // 🕰️
            case BELL -> "\uD83D\uDD14"; // 🔔
            case CAMPFIRE, SOUL_CAMPFIRE -> "\uD83D\uDD25"; // 🔥
            case LANTERN, SOUL_LANTERN -> "\uD83D\uDD2E"; // 🔮
            case CANDLE -> "\uD83D\uDD6F\uFE0F"; // 🕯️

            default -> "●";
        };
    }

    private static String getShortName(Material mat) {
        String name = formatEnum(mat.name());
        // Shorten some common long names
        return switch (mat) {
            case ENCHANTED_GOLDEN_APPLE -> "God Apple";
            case GOLDEN_APPLE -> "Gold Apple";
            case EXPERIENCE_BOTTLE -> "XP Bottle";
            case ENCHANTED_BOOK -> "Book";
            case COOKED_PORKCHOP -> "Cooked Pork";
            case COOKED_CHICKEN -> "Cooked Chicken";
            case COOKED_BEEF -> "Steak";
            case COOKED_MUTTON -> "Cooked Mutton";
            case COOKED_SALMON -> "Cooked Salmon";
            case COOKED_COD -> "Cooked Cod";
            case SPLASH_POTION -> "Splash Potion";
            case LINGERING_POTION -> "Lingering Potion";
            case TIPPED_ARROW -> "Tipped Arrow";
            default -> name;
        };
    }
}
