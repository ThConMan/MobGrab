package com.mobgrab.listener;

import com.mobgrab.MobGrab;
import com.mobgrab.util.EffectUtil;
import com.mobgrab.util.EntitySerializer;
import com.mobgrab.util.MobDataUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class PlaceListener implements Listener {

    private final MobGrab plugin;
    private final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.HashMap<>();

    public PlaceListener(MobGrab plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        boolean isMobGrabItem = MobDataUtil.isMobItem(item);
        boolean isClickVillagerItem = !isMobGrabItem && isClickVillagersItem(item);
        if (!isMobGrabItem && !isClickVillagerItem) return;

        if (!player.hasPermission("mobgrab.place")) {
            player.sendMessage(Component.text("You don't have permission to place mobs.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfigManager().isWorldDisabled(player.getWorld())) {
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && (now - last) < plugin.getConfigManager().getCooldownSeconds() * 1000L) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Block clicked = event.getClickedBlock();
        BlockFace face = event.getBlockFace();
        if (clicked == null) return;

        Location spawnLoc = clicked.getRelative(face).getLocation().add(0.5, 0, 0.5);

        if (!player.hasPermission("mobgrab.bypass.protection")
                && !plugin.getProtectionManager().canBuild(player, spawnLoc)) {
            player.sendMessage(Component.text("You can't place mobs here.", NamedTextColor.RED));
            return;
        }

        var pdc = item.getItemMeta().getPersistentDataContainer();
        String data;
        String typeName;

        if (isClickVillagerItem) {
            data = pdc.get(MobGrab.CV_NBT_KEY, PersistentDataType.STRING);
            typeName = pdc.get(MobGrab.CV_TYPE_KEY, PersistentDataType.STRING);
        } else {
            data = pdc.get(MobGrab.MOB_DATA_KEY, PersistentDataType.STRING);
            typeName = pdc.get(MobGrab.MOB_TYPE_KEY, PersistentDataType.STRING);
        }
        if (data == null || typeName == null) return;

        EntityType type;
        try {
            type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid entity type.", NamedTextColor.RED));
            return;
        }

        // Re-validate on placement: a disabled mob (or one handed/traded after being disabled)
        // must not be placeable. This is also the only enable-gate for ClickVillagers items.
        if (!plugin.getConfigManager().isMobEnabled(type)) {
            player.sendMessage(Component.text("That mob can't be placed here.", NamedTextColor.RED));
            return;
        }

        Integer stackCount = pdc.has(MobGrab.MOB_STACK_KEY, PersistentDataType.INTEGER)
                ? pdc.get(MobGrab.MOB_STACK_KEY, PersistentDataType.INTEGER)
                : null;

        Entity spawned;
        try {
            spawned = EntitySerializer.deserialize(data, type, spawnLoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize entity: " + e.getMessage());
            player.sendMessage(Component.text("Failed to place mob. Please contact an admin.", NamedTextColor.RED));
            return;
        }

        // Guard against forged/corrupt items whose SNBT doesn't match the claimed type.
        if (spawned.getType() != type) {
            spawned.remove();
            player.sendMessage(Component.text("Invalid mob data.", NamedTextColor.RED));
            return;
        }

        if (stackCount != null && stackCount > 1 && spawned instanceof org.bukkit.entity.LivingEntity living) {
            try {
                Class.forName("dev.rosewood.rosestacker.api.RoseStackerAPI");
                com.mobgrab.compat.RoseStackerHook.setStackSize(living, stackCount);
            } catch (ClassNotFoundException ignored) {
                // RoseStacker not present — single mob spawned; warn that the stack count was dropped.
                plugin.getLogger().warning("Placed a stacked mob item (x" + stackCount
                        + ") but RoseStacker is not installed; spawned a single mob.");
                player.sendMessage(Component.text("RoseStacker not installed — placed a single mob.",
                        NamedTextColor.YELLOW));
            }
        }

        EffectUtil.playPlaceEffect(player, spawnLoc);
        item.setAmount(item.getAmount() - 1);
        cooldowns.put(player.getUniqueId(), now);

        String entityName = MobDataUtil.formatEntityName(type);
        Component msg = Component.text("Placed ", NamedTextColor.GREEN);
        if (stackCount != null && stackCount > 1) {
            msg = msg.append(Component.text(stackCount, NamedTextColor.YELLOW))
                    .append(Component.text("\u00D7 ", NamedTextColor.GRAY))
                    .append(Component.text(entityName, NamedTextColor.GOLD))
                    .append(Component.text(" (stack)", NamedTextColor.GRAY));
        } else {
            msg = msg.append(Component.text(entityName, NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.GREEN));
        }
        player.sendMessage(msg);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private boolean isClickVillagersItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(MobGrab.CV_VILLAGER_KEY, PersistentDataType.BOOLEAN);
    }
}
