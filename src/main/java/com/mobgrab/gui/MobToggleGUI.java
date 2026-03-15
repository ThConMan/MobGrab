package com.mobgrab.gui;

import com.mobgrab.MobGrab;
import com.mobgrab.util.HeadUtil;
import com.mobgrab.util.MobDataUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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

    private final MobGrab plugin;
    private final List<EntityType> mobTypes;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

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

    public void open(Player player, int page) {
        // Bedrock players get a form-based GUI
        if (plugin.getGeyserSupport().isBedrockPlayer(player)) {
            plugin.getGeyserSupport().openToggleForm(player, page);
            return;
        }

        int totalPages = getTotalPages();
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, mobTypes.size());

        for (int i = start; i < end; i++) {
            EntityType type = mobTypes.get(i);
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

        // Navigation row
        if (page > 0) {
            inv.setItem(45, createNavItem(Material.ARROW, "Previous Page"));
        }
        inv.setItem(49, createNavItem(Material.PAPER, "Page " + (page + 1) + "/" + totalPages));
        if (page < totalPages - 1) {
            inv.setItem(53, createNavItem(Material.ARROW, "Next Page"));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().title())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        Integer page = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 45 && page > 0) {
            open(player, page - 1);
            return;
        }
        if (slot == 53 && page < getTotalPages() - 1) {
            open(player, page + 1);
            return;
        }
        if (slot >= SLOTS_PER_PAGE) return;

        int index = page * SLOTS_PER_PAGE + slot;
        if (index >= mobTypes.size()) return;

        plugin.getConfigManager().toggleMob(mobTypes.get(index));
        open(player, page);
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) mobTypes.size() / SLOTS_PER_PAGE));
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
