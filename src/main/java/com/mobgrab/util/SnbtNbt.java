package com.mobgrab.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Turns the SNBT text MobGrab stores into the gzipped binary NBT that
 * {@code UnsafeValues#deserializeEntity} reads.
 *
 * <p>That is the only public route into the game's data upgrader, and MobGrab needs it:
 * mob items keep the entity as text inside plugin data, which a world upgrade never touches.
 * When the save format changes between versions (26.3 turned block states from a compound
 * into a string, so an enderman's held block stopped loading), the stored text has to go
 * through the same fixers the world did.
 *
 * <p>Parses everything the game prints as SNBT: compounds, lists, typed arrays, suffixed
 * numbers, quoted and bare strings. Mixed-type lists are written the way the game writes
 * them since 1.21.5, as a list of compounds with each non-compound wrapped under an empty key.
 */
public final class SnbtNbt {

    private static final byte END = 0, BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6,
            BYTE_ARRAY = 7, STRING = 8, LIST = 9, COMPOUND = 10, INT_ARRAY = 11, LONG_ARRAY = 12;

    private SnbtNbt() {}

    /** A parsed tag: its NBT type id and a Java value (Byte, Short, ..., String, List, Map, arrays). */
    record Tag(byte type, Object value) {}

    /**
     * Builds the bytes for an entity: the stored compound plus the {@code id} it was saved
     * without and the {@code DataVersion} it was written at.
     */
    public static byte[] entityBytes(String snbt, String id, int dataVersion) {
        Map<String, Tag> root = parseCompound(snbt);
        root.putIfAbsent("id", new Tag(STRING, id));
        root.put("DataVersion", new Tag(INT, dataVersion));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(COMPOUND);
            out.writeUTF("");
            writeCompound(out, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Tag> parseCompound(String snbt) {
        Parser p = new Parser(snbt);
        Tag tag = p.value();
        p.skipWs();
        if (p.pos != p.s.length()) throw p.error("trailing data");
        if (tag.type() != COMPOUND) throw p.error("not a compound");
        return (Map<String, Tag>) tag.value();
    }

    // ---- writing ---------------------------------------------------------------------------

    private static void writeCompound(DataOutputStream out, Map<String, Tag> map) throws IOException {
        for (Map.Entry<String, Tag> e : map.entrySet()) {
            out.writeByte(e.getValue().type());
            out.writeUTF(e.getKey());
            writePayload(out, e.getValue());
        }
        out.writeByte(END);
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream out, Tag tag) throws IOException {
        switch (tag.type()) {
            case BYTE -> out.writeByte((Byte) tag.value());
            case SHORT -> out.writeShort((Short) tag.value());
            case INT -> out.writeInt((Integer) tag.value());
            case LONG -> out.writeLong((Long) tag.value());
            case FLOAT -> out.writeFloat((Float) tag.value());
            case DOUBLE -> out.writeDouble((Double) tag.value());
            case STRING -> out.writeUTF((String) tag.value());
            case COMPOUND -> writeCompound(out, (Map<String, Tag>) tag.value());
            case BYTE_ARRAY -> { byte[] a = (byte[]) tag.value(); out.writeInt(a.length); out.write(a); }
            case INT_ARRAY -> { int[] a = (int[]) tag.value(); out.writeInt(a.length); for (int v : a) out.writeInt(v); }
            case LONG_ARRAY -> { long[] a = (long[]) tag.value(); out.writeInt(a.length); for (long v : a) out.writeLong(v); }
            case LIST -> writeList(out, (List<Tag>) tag.value());
            default -> throw new IllegalStateException("unknown tag type " + tag.type());
        }
    }

    private static void writeList(DataOutputStream out, List<Tag> list) throws IOException {
        if (list.isEmpty()) {
            out.writeByte(END);
            out.writeInt(0);
            return;
        }
        byte type = list.get(0).type();
        boolean mixed = list.stream().anyMatch(t -> t.type() != type);
        if (!mixed) {
            out.writeByte(type);
            out.writeInt(list.size());
            for (Tag t : list) writePayload(out, t);
            return;
        }
        out.writeByte(COMPOUND);
        out.writeInt(list.size());
        for (Tag t : list) {
            if (t.type() == COMPOUND) {
                writePayload(out, t);
            } else {
                Map<String, Tag> wrapper = new LinkedHashMap<>();
                wrapper.put("", t);
                writeCompound(out, wrapper);
            }
        }
    }

    // ---- parsing ---------------------------------------------------------------------------

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) { this.s = s; }

        IllegalArgumentException error(String what) {
            int from = Math.max(0, pos - 20), to = Math.min(s.length(), pos + 20);
            return new IllegalArgumentException("SNBT " + what + " at " + pos + ": ..." + s.substring(from, to) + "...");
        }

        void skipWs() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }

        char peek() { skipWs(); if (pos >= s.length()) throw error("unexpected end"); return s.charAt(pos); }

        void expect(char c) { if (peek() != c) throw error("expected '" + c + "'"); pos++; }

        Tag value() {
            char c = peek();
            if (c == '{') return compound();
            if (c == '[') return listOrArray();
            if (c == '"' || c == '\'') return new Tag(STRING, quoted());
            return scalar(bare());
        }

        Tag compound() {
            expect('{');
            Map<String, Tag> map = new LinkedHashMap<>();
            if (peek() == '}') { pos++; return new Tag(COMPOUND, map); }
            while (true) {
                char c = peek();
                String key = (c == '"' || c == '\'') ? quoted() : bare();
                if (key.isEmpty() && c != '"' && c != '\'') throw error("empty key");
                expect(':');
                map.put(key, value());
                char next = peek();
                pos++;
                if (next == '}') return new Tag(COMPOUND, map);
                if (next != ',') throw error("expected ',' or '}'");
                if (peek() == '}') { pos++; return new Tag(COMPOUND, map); }
            }
        }

        Tag listOrArray() {
            expect('[');
            skipWs();
            if (pos + 1 < s.length() && s.charAt(pos + 1) == ';'
                    && "BILbil".indexOf(s.charAt(pos)) >= 0) {
                char kind = Character.toUpperCase(s.charAt(pos));
                pos += 2;
                List<Tag> items = elements();
                return switch (kind) {
                    case 'B' -> { byte[] a = new byte[items.size()]; for (int i = 0; i < a.length; i++) a[i] = ((Number) items.get(i).value()).byteValue(); yield new Tag(BYTE_ARRAY, a); }
                    case 'I' -> { int[] a = new int[items.size()]; for (int i = 0; i < a.length; i++) a[i] = ((Number) items.get(i).value()).intValue(); yield new Tag(INT_ARRAY, a); }
                    default -> { long[] a = new long[items.size()]; for (int i = 0; i < a.length; i++) a[i] = ((Number) items.get(i).value()).longValue(); yield new Tag(LONG_ARRAY, a); }
                };
            }
            return new Tag(LIST, elements());
        }

        /** Elements up to and including the closing ']'. The opening bracket is already consumed. */
        List<Tag> elements() {
            List<Tag> items = new ArrayList<>();
            if (peek() == ']') { pos++; return items; }
            while (true) {
                items.add(value());
                char next = peek();
                pos++;
                if (next == ']') return items;
                if (next != ',') throw error("expected ',' or ']'");
                if (peek() == ']') { pos++; return items; }
            }
        }

        String quoted() {
            char q = s.charAt(pos++);
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) throw error("unterminated string");
                char c = s.charAt(pos++);
                if (c == q) return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (pos >= s.length()) throw error("bad escape");
                char e = s.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 's' -> sb.append(' ');
                    case 'x' -> { sb.append((char) Integer.parseInt(s.substring(pos, pos + 2), 16)); pos += 2; }
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; }
                    case 'U' -> { sb.appendCodePoint(Integer.parseInt(s.substring(pos, pos + 8), 16)); pos += 8; }
                    default -> sb.append(e); // \\ \" \' and anything else taken literally
                }
            }
        }

        String bare() {
            skipWs();
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '+') pos++;
                else break;
            }
            return s.substring(start, pos);
        }

        Tag scalar(String word) {
            if (word.isEmpty()) throw error("expected a value");
            if (word.equals("true")) return new Tag(BYTE, (byte) 1);
            if (word.equals("false")) return new Tag(BYTE, (byte) 0);
            char last = Character.toLowerCase(word.charAt(word.length() - 1));
            String body = word.substring(0, word.length() - 1);
            try {
                switch (last) {
                    case 'b': if (isInteger(body)) return new Tag(BYTE, (byte) Long.parseLong(body)); break;
                    case 's': if (isInteger(body)) return new Tag(SHORT, (short) Long.parseLong(body)); break;
                    case 'l': if (isInteger(body)) return new Tag(LONG, Long.parseLong(body)); break;
                    case 'f': if (isDecimal(body)) return new Tag(FLOAT, Float.parseFloat(body)); break;
                    case 'd': if (isDecimal(body)) return new Tag(DOUBLE, Double.parseDouble(body)); break;
                    default: break;
                }
                if (isInteger(word)) {
                    long v = Long.parseLong(word);
                    if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return new Tag(INT, (int) v);
                }
                if (isDecimal(word) && (word.contains(".") || word.contains("e") || word.contains("E"))) {
                    return new Tag(DOUBLE, Double.parseDouble(word));
                }
            } catch (NumberFormatException ignored) {
                // falls through to a plain string, as the game does for anything not numeric
            }
            return new Tag(STRING, word);
        }

        static boolean isInteger(String v) { return v.matches("[+-]?\\d+"); }

        static boolean isDecimal(String v) { return v.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?"); }
    }
}
