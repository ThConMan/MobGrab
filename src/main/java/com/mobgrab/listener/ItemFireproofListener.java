package com.mobgrab.listener;

import com.mobgrab.MobGrab;
import com.mobgrab.util.MobDataUtil;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Stops mob items from burning in fire/lava when fireproof-items is on. Checked against the
 * current config at damage time, so toggling it applies to items already in the world.
 */
public final class ItemFireproofListener implements Listener {

    private final MobGrab plugin;

    public ItemFireproofListener(MobGrab plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!plugin.getConfigManager().isFireproofItems()) return;

        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> {
                if (MobDataUtil.isMobItem(item.getItemStack())) {
                    event.setCancelled(true);
                }
            }
            default -> { /* not a fire/lava cause — leave it alone */ }
        }
    }
}
