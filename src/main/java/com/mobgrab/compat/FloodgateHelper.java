package com.mobgrab.compat;

import com.mobgrab.MobGrab;
import com.mobgrab.util.MobDataUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.UUID;

final class FloodgateHelper {

    private FloodgateHelper() {}

    static boolean isFloodgatePlayer(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    static void sendToggleForm(MobGrab plugin, Player player, List<EntityType> mobTypes,
                               int start, int end, int page, int totalPages, int perPage) {

        SimpleForm.Builder form = SimpleForm.builder()
                .title("MobGrab Settings (Page " + (page + 1) + "/" + totalPages + ")");

        for (int i = start; i < end; i++) {
            EntityType type = mobTypes.get(i);
            boolean enabled = plugin.getConfigManager().isMobEnabled(type);
            String label = (enabled ? "\u00A7a\u2714 " : "\u00A7c\u2716 ") + MobDataUtil.formatEntityName(type);
            form.button(label);
        }

        if (page > 0) {
            form.button("\u00A7e\u25C0 Previous Page");
        }
        if (page < totalPages - 1) {
            form.button("\u00A7e\u25B6 Next Page");
        }

        final int currentPage = page;
        final int mobCount = end - start;

        form.validResultHandler(response -> {
            int buttonId = response.clickedButtonId();

            if (buttonId < mobCount) {
                int index = currentPage * perPage + buttonId;
                if (index < mobTypes.size()) {
                    plugin.getConfigManager().toggleMob(mobTypes.get(index));
                }
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.getGeyserSupport().openToggleForm(player, currentPage));
            } else {
                int navIndex = buttonId - mobCount;
                int nextPage = (currentPage > 0 && navIndex == 0) ? currentPage - 1 : currentPage + 1;
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.getGeyserSupport().openToggleForm(player, nextPage));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }
}
