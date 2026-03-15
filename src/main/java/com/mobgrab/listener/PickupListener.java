package com.mobgrab.listener;

import com.mobgrab.MobGrab;
import com.mobgrab.config.ConfigManager;
import com.mobgrab.util.EffectUtil;
import com.mobgrab.util.MobDataUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PickupListener implements Listener {

    private final MobGrab plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PickupListener(MobGrab plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (entity instanceof Player) return;
        if (!(entity instanceof LivingEntity)) return;
        if (!player.isSneaking()) return;

        ConfigManager config = plugin.getConfigManager();

        if (!config.isMobEnabled(entity.getType())) {
            player.sendMessage(Component.text("This mob cannot be picked up.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        String permNode = "mobgrab.pickup." + entity.getType().name().toLowerCase();
        if (!player.hasPermission("mobgrab.pickup.*") && !player.hasPermission(permNode)) {
            player.sendMessage(Component.text("You don't have permission to pick up this mob.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("mobgrab.bypass.protection")
                && !plugin.getProtectionManager().canBuild(player, entity.getLocation())) {
            player.sendMessage(Component.text("You can't pick up mobs here.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && (now - last) < config.getCooldownSeconds() * 1000L) {
            event.setCancelled(true);
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);

        int stackSize = 1;
        boolean handledByStacker = false;
        if (entity instanceof LivingEntity living) {
            try {
                Class.forName("dev.rosewood.rosestacker.api.RoseStackerAPI");
                if (com.mobgrab.compat.RoseStackerHook.isStacked(living)) {
                    if (config.isRoseStackerPickupWholeStack()) {
                        stackSize = com.mobgrab.compat.RoseStackerHook.getStackSize(living);
                    }
                    handledByStacker = true;
                }
            } catch (ClassNotFoundException ignored) {}
        }

        ItemStack mobItem = MobDataUtil.createMobItem(entity, stackSize);
        EffectUtil.playPickupEffect(player, entity.getLocation());

        if (handledByStacker && entity instanceof LivingEntity living) {
            if (config.isRoseStackerPickupWholeStack()) {
                com.mobgrab.compat.RoseStackerHook.removeStack(living);
            } else {
                com.mobgrab.compat.RoseStackerHook.decreaseStack(living);
            }
        } else if (!handledByStacker) {
            entity.remove();
        }

        var overflow = player.getInventory().addItem(mobItem);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        String entityName = MobDataUtil.formatEntityName(entity.getType());
        Component msg = Component.text("Picked up ", NamedTextColor.GREEN);
        if (stackSize > 1) {
            msg = msg.append(Component.text(stackSize, NamedTextColor.YELLOW))
                    .append(Component.text("\u00D7 ", NamedTextColor.GRAY))
                    .append(Component.text(entityName, NamedTextColor.GOLD))
                    .append(Component.text(" (stack)", NamedTextColor.GRAY));
        } else {
            msg = msg.append(Component.text(entityName, NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.GREEN));
        }
        player.sendMessage(msg);
    }
}
