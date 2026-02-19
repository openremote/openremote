package org.openremote.container.timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PseudoClockTest {

    private TimerService.Clock clock;

    @BeforeEach
    public void setup() {
        clock = TimerService.Clock.PSEUDO;
        clock.init();
        clock.stop();
    }
    @Test
    public void testSetTime() {
        clock.setTime(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneId.of("UTC"));
        assertEquals(0, clock.getCurrentTimeMillis());

        clock.setTime(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0), ZoneId.of("UTC"));
        assertEquals(24 * 3_600_000, clock.getCurrentTimeMillis());

        clock.setTime(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneId.of("CET"));
        assertEquals(-3_600_000, clock.getCurrentTimeMillis());

        clock.setTime(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0), ZoneId.of("CET"));
        assertEquals(23 * 3_600_000, clock.getCurrentTimeMillis());
    }

    @Test
    public void testSetTimeISO() {
        clock.setTime("1970-01-01T00:00:00.000Z");
        assertEquals(0, clock.getCurrentTimeMillis());

        clock.setTime("1970-01-02T00:00:00.000Z");
        assertEquals(24 * 3_600_000, clock.getCurrentTimeMillis());

        clock.setTime("1970-01-01T00:00:00.000+01:00");
        assertEquals(-3_600_000, clock.getCurrentTimeMillis());

        clock.setTime("1970-01-02T00:00:00.000+01:00");
        assertEquals(23 * 3_600_000, clock.getCurrentTimeMillis());
    }

}
