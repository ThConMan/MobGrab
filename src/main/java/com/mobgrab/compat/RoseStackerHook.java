package com.mobgrab.compat;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedEntity;
import org.bukkit.entity.LivingEntity;

public final class RoseStackerHook {

    public static boolean isStacked(LivingEntity entity) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        return api.isEntityStacked(entity) && api.getStackedEntity(entity).getStackSize() > 1;
    }

    public static int getStackSize(LivingEntity entity) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        StackedEntity stacked = api.getStackedEntity(entity);
        return stacked != null ? stacked.getStackSize() : 1;
    }

    public static void decreaseStack(LivingEntity entity) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        StackedEntity stacked = api.getStackedEntity(entity);
        if (stacked != null && stacked.getStackSize() > 1) {
            stacked.decreaseStackSize();
        }
    }

    public static void removeStack(LivingEntity entity) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        StackedEntity stacked = api.getStackedEntity(entity);
        if (stacked != null) {
            api.removeEntityStack(stacked);
        }
        entity.remove();
    }

    public static void setStackSize(LivingEntity entity, int count) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        StackedEntity stacked = api.createEntityStack(entity, false);
        if (stacked != null && count > 1) {
            stacked.increaseStackSize(count - 1, true);
        }
    }
}
