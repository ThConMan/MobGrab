package com.mobgrab.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnbtNbtTest {

    /** Reads the bytes back with an independent NBT reader, so the writer is checked end to end. */
    private static Map<String, Object> read(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
            assertEquals(10, in.readByte());
            assertEquals("", in.readUTF());
            return compound(in);
        }
    }

    private static Map<String, Object> compound(DataInputStream in) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == 0) return map;
            String key = in.readUTF();
            map.put(key, payload(in, type));
        }
    }

    private static Object payload(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { byte[] a = new byte[in.readInt()]; in.readFully(a); yield a; }
            case 8 -> in.readUTF();
            case 9 -> {
                byte et = in.readByte();
                int n = in.readInt();
                List<Object> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(payload(in, et));
                yield l;
            }
            case 10 -> compound(in);
            case 11 -> { int[] a = new int[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readInt(); yield a; }
            case 12 -> { long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readLong(); yield a; }
            default -> throw new IOException("bad type " + type);
        };
    }

    @Test
    void addsIdAndDataVersion() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes("{Health:8.0f}", "minecraft:sheep", 4903));
        assertEquals("minecraft:sheep", m.get("id"));
        assertEquals(4903, m.get("DataVersion"));
        assertEquals(8.0f, m.get("Health"));
    }

    @Test
    void keepsAnExistingId() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes("{id:\"minecraft:cow\"}", "minecraft:sheep", 1));
        assertEquals("minecraft:cow", m.get("id"));
    }

    @Test
    void everyNumberSuffixKeepsItsType() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes(
                "{a:1b,b:300s,c:7,d:5L,e:0.5f,f:0.25d,g:-3B,h:1.5,i:1e3,j:true,k:false}", "x:y", 1));
        assertEquals((byte) 1, m.get("a"));
        assertEquals((short) 300, m.get("b"));
        assertEquals(7, m.get("c"));
        assertEquals(5L, m.get("d"));
        assertEquals(0.5f, m.get("e"));
        assertEquals(0.25d, m.get("f"));
        assertEquals((byte) -3, m.get("g"));
        assertEquals(1.5d, m.get("h"));
        assertEquals(1000d, m.get("i"));
        assertEquals((byte) 1, m.get("j"));
        assertEquals((byte) 0, m.get("k"));
    }

    @Test
    void typedArrays() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes(
                "{UUID:[I;-361917968,-1770567171,1,2],b:[B;1b,2b],l:[L;9L],e:[I;]}", "x:y", 1));
        assertArrayEquals(new int[]{-361917968, -1770567171, 1, 2}, (int[]) m.get("UUID"));
        assertArrayEquals(new byte[]{1, 2}, (byte[]) m.get("b"));
        assertArrayEquals(new long[]{9L}, (long[]) m.get("l"));
        assertArrayEquals(new int[0], (int[]) m.get("e"));
    }

    @Test
    void stringsQuotesAndEscapes() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes(
                "{a:\"say \\\"hi\\\"\",b:'it\\'s',c:\"back\\\\slash\",\"odd key\":\"minecraft:stone\",d:\"\\u00e9\"}", "x:y", 1));
        assertEquals("say \"hi\"", m.get("a"));
        assertEquals("it's", m.get("b"));
        assertEquals("back\\slash", m.get("c"));
        assertEquals("minecraft:stone", m.get("odd key"));
        assertEquals("\u00e9", m.get("d"));
    }

    @Test
    void bareWordsThatLookNumericStayStrings() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes("{a:add_value,b:1.2.3,c:5x}", "x:y", 1));
        assertEquals("add_value", m.get("a"));
        assertEquals("1.2.3", m.get("b"));
        assertEquals("5x", m.get("c"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestedListsAndCompounds() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes(
                "{Motion:[0.0d,1.0d,0.0d],attributes:[{base:16.0d,id:\"minecraft:follow_range\",modifiers:[]}],empty:[]}", "x:y", 1));
        assertEquals(List.of(0.0d, 1.0d, 0.0d), m.get("Motion"));
        Map<String, Object> attr = (Map<String, Object>) ((List<Object>) m.get("attributes")).get(0);
        assertEquals("minecraft:follow_range", attr.get("id"));
        assertEquals(List.of(), attr.get("modifiers"));
        assertEquals(List.of(), m.get("empty"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mixedListsAreWrappedTheWayTheGameDoes() throws IOException {
        Map<String, Object> m = read(SnbtNbt.entityBytes("{l:[1,\"two\",{three:3}]}", "x:y", 1));
        List<Object> l = (List<Object>) m.get("l");
        assertEquals(Map.of("", 1), l.get(0));
        assertEquals(Map.of("", "two"), l.get(1));
        assertEquals(Map.of("three", 3), l.get(2));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> SnbtNbt.entityBytes("{a:1", "x:y", 1));
        assertThrows(IllegalArgumentException.class, () -> SnbtNbt.entityBytes("[1,2]", "x:y", 1));
        assertThrows(IllegalArgumentException.class, () -> SnbtNbt.entityBytes("{a:1}}", "x:y", 1));
    }
}
