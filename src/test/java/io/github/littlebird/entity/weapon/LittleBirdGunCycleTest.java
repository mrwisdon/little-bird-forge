package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LittleBirdGunCycleTest {

    private static final class Sim {
        private LittleBirdGunCycle.Snapshot snapshot = LittleBirdGunCycle.Snapshot.initial();
        private final List<Integer> barrels = new ArrayList<>();
        private int ticks;
        private int firstFireTick = -1;

        private void tick(boolean held) {
            ticks++;
            LittleBirdGunCycle.Step step = LittleBirdGunCycle.advance(held, snapshot);
            snapshot = step.snapshot();
            if (step.fire()) {
                if (firstFireTick < 0) {
                    firstFireTick = ticks;
                }
                barrels.add(step.barrel());
            }
        }
    }

    @Test
    void spinUpTakesEightTicksThenFires() {
        Sim sim = new Sim();
        for (int i = 0; i < LittleBirdGunCycle.WARMUP_TICKS - 1; i++) {
            sim.tick(true);
        }
        assertEquals(0, sim.barrels.size());
        assertEquals(LittleBirdGunCycle.WARMING, sim.snapshot.state());
        sim.tick(true);
        assertEquals(LittleBirdGunCycle.WARMUP_TICKS, sim.firstFireTick);
        assertEquals(1, sim.barrels.size());
        assertEquals(LittleBirdGunCycle.FIRING, sim.snapshot.state());
    }

    @Test
    void firesTwentyRoundsPerSecondAlternatingBarrels() {
        Sim sim = new Sim();
        for (int i = 0; i < 26; i++) {
            sim.tick(true);
        }
        assertEquals(19, sim.barrels.size(),
                "one round per tick after the 8 tick spin-up, got " + sim.barrels);
        for (int i = 0; i < sim.barrels.size(); i++) {
            assertEquals(i % 2, sim.barrels.get(i).intValue());
        }
    }

    @Test
    void heatBuildsPerShotAndOverheatsAfterSixSeconds() {
        Sim sim = new Sim();
        int overheatTick = -1;
        for (int i = 0; i < 400 && overheatTick < 0; i++) {
            sim.tick(true);
            if (sim.snapshot.state() == LittleBirdGunCycle.OVERHEATED) {
                overheatTick = sim.ticks;
            }
        }
        assertTrue(overheatTick > 0, "gun never overheated");
        assertEquals(119, sim.barrels.size(),
                "one heat per round means 119 rounds before the 120-heat cap");
        assertEquals(120, sim.snapshot.heat());
        assertTrue(sim.ticks >= 120 && sim.ticks <= 130,
                "about six seconds of continuous fire, was " + sim.ticks + " ticks");
    }

    @Test
    void overheatedGunStaysLockedAndSilent() {
        Sim sim = new Sim();
        while (sim.snapshot.state() != LittleBirdGunCycle.OVERHEATED) {
            sim.tick(true);
        }
        int lockedRounds = sim.barrels.size();
        for (int i = 0; i < LittleBirdGunCycle.OVERHEAT_LOCK_TICKS - 1; i++) {
            sim.tick(true);
            assertEquals(lockedRounds, sim.barrels.size(), "fired while overheated");
        }
        assertTrue(sim.snapshot.heat() < LittleBirdGunCycle.MAX_HEAT);
    }

    @Test
    void releasingTheTriggerCoolsTheBarrels() {
        Sim sim = new Sim();
        for (int i = 0; i < 40; i++) {
            sim.tick(true);
        }
        int heatBefore = sim.snapshot.heat();
        assertTrue(heatBefore > 20, "heat was " + heatBefore);
        for (int i = 0; i < 10; i++) {
            sim.tick(false);
        }
        assertEquals(LittleBirdGunCycle.IDLE, sim.snapshot.state());
        assertEquals(heatBefore - 10 * LittleBirdGunCycle.COOL_PER_TICK, sim.snapshot.heat());
    }

    @Test
    void releasingAfterOverheatReturnsToIdleOnceUnlocked() {
        Sim sim = new Sim();
        while (sim.snapshot.state() != LittleBirdGunCycle.OVERHEATED) {
            sim.tick(true);
        }
        for (int i = 0; i < LittleBirdGunCycle.OVERHEAT_LOCK_TICKS + 1; i++) {
            sim.tick(false);
        }
        assertEquals(LittleBirdGunCycle.IDLE, sim.snapshot.state());
        assertFalse(sim.snapshot.heat() >= LittleBirdGunCycle.MAX_HEAT);
    }

    @Test
    void nullSnapshotIsTolerated() {
        LittleBirdGunCycle.Step step = LittleBirdGunCycle.advance(false, null);
        assertEquals(LittleBirdGunCycle.IDLE, step.snapshot().state());
        assertFalse(step.fire());
    }
}
