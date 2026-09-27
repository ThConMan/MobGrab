package com.mobgrab.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class EntitySerializer {

    /**
     * The data version of Paper 26.2, which is what wrote every item and preset from before
     * MobGrab stored a version (2.2.0 required 26.2). Anything unversioned is upgraded from here.
     */
    public static final int LEGACY_DATA_VERSION = 4903;

    private EntitySerializer() {}

    /** The data version this server writes, to store next to anything {@link #serialize} returns. */
    public static int currentDataVersion() {
        return Bukkit.getUnsafe().getDataVersion();
    }

    public static String serialize(Entity entity) {
        EntitySnapshot snapshot = entity.createSnapshot();
        // RoseStacker (and other stacker plugins) persist the whole stack inside the
        // entity's persistent-data container. If we serialize that verbatim, placing the
        // item re-inflates the stack on spawn — so one picked-up mob duplicates back into
        // N. Strip those PDC keys; MobGrab drives the stack count itself via MOB_STACK_KEY.
        String snbt = snapshot.getAsString();
        snbt = stripPdcNamespace(snbt, "rosestacker");
        return snbt;
    }

    /**
     * Spawns the mob stored as {@code snbt}. {@code dataVersion} is the version it was written
     * at, or null for data from before MobGrab recorded one.
     *
     * <p>The text sits inside plugin data, which a world upgrade never touches, so older data
     * is run through the game's upgrader here, exactly as the world's own entities were.
     * Without this, anything whose save format changed is silently dropped: 26.3 turned block
     * states into strings, and an enderman picked up on 26.2 lost the block it was holding.
     */
    public static Entity deserialize(String snbt, EntityType type, Location location, Integer dataVersion) {
        // Also strip on the way in so items created before this fix (which still carry the
        // embedded stack NBT) place as a single clean mob instead of re-inflating the stack.
        snbt = stripPdcNamespace(snbt, "rosestacker");
        int from = dataVersion != null ? dataVersion : LEGACY_DATA_VERSION;
        if (from >= currentDataVersion()) {
            EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(snbt);
            return snapshot.createEntity(location);
        }
        byte[] nbt = SnbtNbt.entityBytes(snbt, type.getKey().toString(), from);
        Entity entity = Bukkit.getUnsafe().deserializeEntity(nbt, location.getWorld(), false, true);
        // Keep the facing it was saved with, as the snapshot path does.
        Location at = location.clone();
        at.setYaw(entity.getYaw());
        at.setPitch(entity.getPitch());
        if (!entity.spawnAt(at, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("spawn was cancelled");
        }
        return entity;
    }

    /**
     * Removes every persistent-data entry whose key is in the given namespace from an SNBT
     * string (e.g. {@code "rosestacker:stacked_entity"}). Bracket- and quote-aware so it
     * correctly skips byte arrays, lists, compounds, quoted strings, and scalar values.
     */
    static String stripPdcNamespace(String snbt, String namespace) {
        if (snbt == null || snbt.isEmpty()) return snbt;
        String marker = "\"" + namespace + ":";
        StringBuilder sb = new StringBuilder(snbt.length());
        int i = 0;
        while (i < snbt.length()) {
            if (snbt.startsWith(marker, i)) {
                int keyEnd = endOfQuotedString(snbt, i);
                int j = keyEnd;
                while (j < snbt.length() && Character.isWhitespace(snbt.charAt(j))) j++;
                if (j < snbt.length() && snbt.charAt(j) == ':') {
                    j++; // skip the key/value separator
                    while (j < snbt.length() && Character.isWhitespace(snbt.charAt(j))) j++;
                    int valEnd = endOfValue(snbt, j);
                    int k = valEnd;
                    while (k < snbt.length() && Character.isWhitespace(snbt.charAt(k))) k++;
                    if (k < snbt.length() && snbt.charAt(k) == ',') {
                        k++; // drop the trailing separator with the entry
                    } else {
                        // last entry in the compound — drop the preceding comma instead
                        trimTrailingComma(sb);
                    }
                    i = k;
                    continue;
                }
            }
            sb.append(snbt.charAt(i));
            i++;
        }
        return sb.toString();
    }

    /** Returns the index just past the closing quote of a quoted string starting at {@code start}. */
    private static int endOfQuotedString(String s, int start) {
        int i = start + 1;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (ch == '\\') { i += 2; continue; }
            if (ch == '"') return i + 1;
            i++;
        }
        return i;
    }

    /** Returns the index just past an SNBT value (quoted string, {…}, […], or scalar) at {@code start}. */
    private static int endOfValue(String s, int start) {
        if (start >= s.length()) return start;
        char c = s.charAt(start);
        if (c == '"') return endOfQuotedString(s, start);
        if (c == '{' || c == '[') {
            int depth = 0;
            boolean inStr = false;
            for (int i = start; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (inStr) {
                    if (ch == '\\') { i++; continue; }
                    if (ch == '"') inStr = false;
                } else if (ch == '"') {
                    inStr = true;
                } else if (ch == '{' || ch == '[') {
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    depth--;
                    if (depth == 0) return i + 1;
                }
            }
            return s.length();
        }
        // scalar — runs until the next top-level separator or container close
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ',' || ch == '}' || ch == ']') return i;
        }
        return s.length();
    }

    private static void trimTrailingComma(StringBuilder sb) {
        int end = sb.length();
        while (end > 0 && Character.isWhitespace(sb.charAt(end - 1))) end--;
        if (end > 0 && sb.charAt(end - 1) == ',') {
            sb.delete(end - 1, sb.length());
        }
    }
}
