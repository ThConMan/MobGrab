package com.mobgrab.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;

public final class EntitySerializer {

    private EntitySerializer() {}

    public static String serialize(Entity entity) {
        EntitySnapshot snapshot = entity.createSnapshot();
        return snapshot.getAsString();
    }

    public static Entity deserialize(String snbt, EntityType type, Location location) {
        EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(snbt);
        return snapshot.createEntity(location);
    }
}
