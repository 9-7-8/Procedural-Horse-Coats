package com.example.horsegenetics.common.horse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SexTest {

    @Test
    void horseLabels() {
        assertEquals("Stallion", Sex.MALE.label(true));
        assertEquals("Colt", Sex.MALE.label(false));
        assertEquals("Mare", Sex.FEMALE.label(true));
        assertEquals("Filly", Sex.FEMALE.label(false));
    }
}
