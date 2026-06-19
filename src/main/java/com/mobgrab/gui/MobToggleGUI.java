package com.mobgrab.gui;

import com.mobgrab.MobGrab;
import com.mobgrab.util.HeadUtil;
import com.mobgrab.util.MobDataUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class MobToggleGUI implements Listener {

    private static final int SLOTS_PER_PAGE = 45;
    private static final Component TITLE = Component.text("MobGrab Settings", NamedTextColor.DARK_GREEN);

    // Control slots (bottom row).
    private static final int SLOT_PREV = 45;
    private static final int SLOT_CATEGORY = 46;
    private static final int SLOT_FIREPROOF = 47;
    private static final int SLOT_ENABLE_ALL = 48;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_DISABLE_ALL = 50;
    private static final int SLOT_NEXT = 53;

    private enum Category {
        ALL("All", Material.CHEST),
        PASSIVE("Passive", Material.WHEAT),
        HOSTILE("Hostile", Material.IRON_SWORD),
        UTILITY("Utility", Material.IRON_BLOCK),
        VILLAGER("Villager", Material.EMERALD),
        BOSS("Boss", Material.NETHER_STAR);

        final String label;
        final Material icon;
        Category(String label, Material icon) { this.label = label; this.icon = icon; }
        Category next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final MobGrab plugin;
    private final List<EntityType> mobTypes;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, Category> playerCategory = new HashMap<>();

    public MobToggleGUI(MobGrab plugin) {
        this.plugin = plugin;
        this.mobTypes = new ArrayList<>();

        for (EntityType type : EntityType.values()) {
            if (type.getEntityClass() != null
                    && LivingEntity.class.isAssignableFrom(type.getEntityClass())
                    && type != EntityType.PLAYER
                    && type != EntityType.ARMOR_STAND) {
                mobTypes.add(type);
            }
        }
        mobTypes.sort(Comparator.comparing(Enum::name));
    }

    public List<EntityType> getMobTypes() {
        return Collections.unmodifiableList(mobTypes);
    }

    private Category categoryOf(EntityType type) {
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER
                || type == EntityType.WARDEN || type == EntityType.GIANT) {
            return Category.BOSS;
        }
        Class<?> c = type.getEntityClass();
        if (c == null) return Category.PASSIVE;
        if (type == EntityType.VILLAGER || type == EntityType.ZOMBIE_VILLAGER
                || AbstractVillager.class.isAssignableFrom(c)) {
            return Category.VILLAGER;
        }
        if (Golem.class.isAssignableFrom(c) || type == EntityType.MANNEQUIN) {
            return Category.UTILITY;
        }
        if (Monster.class.isAssignableFrom(c)) {
            return Category.HOSTILE;
        }
        return Category.PASSIVE;
    }

    private List<EntityType> filtered(Category category) {
        if (category == Category.ALL) return mobTypes;
        List<EntityType> out = new ArrayList<>();
        for (EntityType t : mobTypes) {
            if (categoryOf(t) == category) out.add(t);
        }
        return out;
    }

    public void open(Player player, int page) {
        // Bedrock players get a form-based GUI
        if (plugin.getGeyserSupport().isBedrockPlayer(player)) {
            plugin.getGeyserSupport().openToggleForm(player, page);
            return;
        }

        Category category = playerCategory.getOrDefault(player.getUniqueId(), Category.ALL);
        List<EntityType> list = filtered(category);

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / SLOTS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, list.size());

        for (int i = start; i < end; i++) {
            EntityType type = list.get(i);
            boolean enabled = plugin.getConfigManager().isMobEnabled(type);

            ItemStack icon = HeadUtil.getMobHead(type);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(MobDataUtil.formatEntityName(type),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(enabled ? "ENABLED" : "DISABLED",
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to toggle", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            icon.setItemMeta(meta);
            inv.setItem(i - start, icon);
        }

        // Control row
        if (page > 0) {
            inv.setItem(SLOT_PREV, createNavItem(Material.ARROW, "Previous Page"));
        }
        inv.setItem(SLOT_PAGE, createNavItem(Material.PAPER, "Page " + (page + 1) + "/" + totalPages));
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, createNavItem(Material.ARROW, "Next Page"));
        }
        inv.setItem(SLOT_CATEGORY, createCategoryItem(category, list.size()));
        inv.setItem(SLOT_FIREPROOF, createFireproofToggle());
        inv.setItem(SLOT_ENABLE_ALL, createBulkItem(Material.LIME_DYE, "Enable all (" + category.label + ")"));
        inv.setItem(SLOT_DISABLE_ALL, createBulkItem(Material.GRAY_DYE, "Disable all (" + category.label + ")"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().title())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        UUID id = player.getUniqueId();
        Category category = playerCategory.getOrDefault(id, Category.ALL);
        List<EntityType> list = filtered(category);
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / SLOTS_PER_PAGE));
        int page = Math.min(playerPages.getOrDefault(id, 0), totalPages - 1);

        switch (slot) {
            case SLOT_PREV -> {
                if (page > 0) open(player, page - 1);
                return;
            }
            case SLOT_NEXT -> {
                if (page < totalPages - 1) open(player, page + 1);
                return;
            }
            case SLOT_CATEGORY -> {
                playerCategory.put(id, category.next());
                open(player, 0);
                return;
            }
            case SLOT_FIREPROOF -> {
                var cm = plugin.getConfigManager();
                cm.setFireproofItems(!cm.isFireproofItems());
                open(player, page);
                return;
            }
            case SLOT_ENABLE_ALL -> {
                for (EntityType t : list) plugin.getConfigManager().setMobEnabled(t, true);
                open(player, page);
                return;
            }
            case SLOT_DISABLE_ALL -> {
                for (EntityType t : list) plugin.getConfigManager().setMobEnabled(t, false);
                open(player, page);
                return;
            }
            default -> { /* fall through to mob slots */ }
        }

        if (slot >= SLOTS_PER_PAGE) return;

        int index = page * SLOTS_PER_PAGE + slot;
        if (index >= list.size()) return;

        plugin.getConfigManager().toggleMob(list.get(index));
        open(player, page);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        playerPages.remove(event.getPlayer().getUniqueId());
        playerCategory.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack createCategoryItem(Category category, int count) {
        ItemStack item = new ItemStack(category.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Category: " + category.label, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(count + " mobs", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to change filter", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFireproofToggle() {
        boolean on = plugin.getConfigManager().isFireproofItems();
        ItemStack item = new ItemStack(on ? Material.NETHERITE_INGOT : Material.BUCKET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Fireproof Items: " + (on ? "ON" : "OFF"),
                on ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(on ? "Mob items survive fire & lava" : "Mob items burn normally",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to toggle", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createBulkItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
