package com.mobgrab.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobDataUtilTest {

    @Test
    void formatEnum_titleCasesUnderscoreNames() {
        assertEquals("Cow", MobDataUtil.formatEnum("COW"));
        assertEquals("Sulfur Cube", MobDataUtil.formatEnum("SULFUR_CUBE"));
        assertEquals("Zombie Nautilus", MobDataUtil.formatEnum("ZOMBIE_NAUTILUS"));
        assertEquals("Camel Husk", MobDataUtil.formatEnum("CAMEL_HUSK"));
    }
}
