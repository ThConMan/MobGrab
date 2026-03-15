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

        if (stackCount != null && stackCount > 1 && spawned instanceof org.bukkit.entity.LivingEntity living) {
            try {
                Class.forName("dev.rosewood.rosestacker.api.RoseStackerAPI");
                com.mobgrab.compat.RoseStackerHook.setStackSize(living, stackCount);
            } catch (ClassNotFoundException ignored) {
                // RoseStacker not present — single mob already spawned above
            }
        }

        EffectUtil.playPlaceEffect(player, spawnLoc);
        item.setAmount(item.getAmount() - 1);

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

    private boolean isClickVillagersItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(MobGrab.CV_VILLAGER_KEY, PersistentDataType.BOOLEAN);
    }
}
