package com.mobgrab.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntitySerializerTest {

    @Test
    void stripsRoseStackerByteArrayEntryInTheMiddle() {
        String snbt = "{Health:4.0f,BukkitValues:{\"rosestacker:stacked_entity\":[B;1B,2B,3B],"
                + "\"minecraft:custom\":1}}";
        String out = EntitySerializer.stripPdcNamespace(snbt, "rosestacker");
        assertEquals("{Health:4.0f,BukkitValues:{\"minecraft:custom\":1}}", out);
    }

    @Test
    void stripsRoseStackerEntryAtEndOfCompound() {
        String snbt = "{BukkitValues:{\"minecraft:custom\":1,\"rosestacker:total_size\":2}}";
        String out = EntitySerializer.stripPdcNamespace(snbt, "rosestacker");
        assertEquals("{BukkitValues:{\"minecraft:custom\":1}}", out);
    }

    @Test
    void stripsMultipleRoseStackerEntries() {
        String snbt = "{BukkitValues:{\"rosestacker:a\":[B;1B],\"minecraft:keep\":5,"
                + "\"rosestacker:b\":\"x\"}}";
        String out = EntitySerializer.stripPdcNamespace(snbt, "rosestacker");
        assertEquals("{BukkitValues:{\"minecraft:keep\":5}}", out);
    }

    @Test
    void stripsRoseStackerEntryWithNestedCompoundValue() {
        String snbt = "{BukkitValues:{\"rosestacker:stack\":{a:1,b:[I;1,2]},\"minecraft:keep\":1}}";
        String out = EntitySerializer.stripPdcNamespace(snbt, "rosestacker");
        assertEquals("{BukkitValues:{\"minecraft:keep\":1}}", out);
    }

    @Test
    void leavesSnbtWithoutNamespaceUntouched() {
        String snbt = "{Health:20.0f,BukkitValues:{\"minecraft:custom\":1}}";
        assertEquals(snbt, EntitySerializer.stripPdcNamespace(snbt, "rosestacker"));
    }

    @Test
    void roseStackerKeyIsGoneEvenAsSoleEntry() {
        String snbt = "{BukkitValues:{\"rosestacker:stacked_entity\":[B;9B,8B,7B]}}";
        String out = EntitySerializer.stripPdcNamespace(snbt, "rosestacker");
        assertEquals("{BukkitValues:{}}", out);
        assertFalse(out.contains("rosestacker"));
    }
}
