package dev.quill.core.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComboCounterTest {
    @Test
    void sameTargetExtendsCombo() {
        ComboCounter c = new ComboCounter(2000);
        c.hit("a", 1000);
        c.hit("a", 1500);
        c.hit("a", 1900);
        assertEquals(3, c.count(1950));
        assertEquals(3, c.best());
    }

    @Test
    void differentTargetStartsNewCombo() {
        ComboCounter c = new ComboCounter(2000);
        c.hit("a", 1000);
        c.hit("a", 1200);
        c.hit("b", 1400);
        assertEquals(1, c.count(1450));
        assertEquals(2, c.best());
    }

    @Test
    void timeoutEndsCombo() {
        ComboCounter c = new ComboCounter(1000);
        c.hit("a", 1000);
        assertEquals(1, c.count(1500));
        assertEquals(0, c.count(2500));
        c.hit("a", 3000);
        assertEquals(1, c.count(3000));
    }

    @Test
    void interruptEndsComboButKeepsBest() {
        ComboCounter c = new ComboCounter(2000);
        c.hit("a", 1000);
        c.hit("a", 1100);
        c.interrupt();
        assertEquals(0, c.count(1200));
        assertEquals(2, c.best());
    }
}
