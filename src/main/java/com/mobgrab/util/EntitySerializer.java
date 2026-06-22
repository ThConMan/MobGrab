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
        // RoseStacker (and other stacker plugins) persist the whole stack inside the
        // entity's persistent-data container. If we serialize that verbatim, placing the
        // item re-inflates the stack on spawn — so one picked-up mob duplicates back into
        // N. Strip those PDC keys; MobGrab drives the stack count itself via MOB_STACK_KEY.
        String snbt = snapshot.getAsString();
        snbt = stripPdcNamespace(snbt, "rosestacker");
        return snbt;
    }

    public static Entity deserialize(String snbt, EntityType type, Location location) {
        // Also strip on the way in so items created before this fix (which still carry the
        // embedded stack NBT) place as a single clean mob instead of re-inflating the stack.
        snbt = stripPdcNamespace(snbt, "rosestacker");
        EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(snbt);
        return snapshot.createEntity(location);
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
