package org.openremote.agent.protocol.simulator;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulatorAgentProtocolScheduleTest {

    private final static long HOUR = 3600;
    private final static long DAY = HOUR * 24;

    @Test
    public void shouldReturnTimeSinceOccurrenceStart() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDate.of(1970, 1, 1), null);

        assertEquals(0, schedule.advanceToNextOccurrence(0));
        assertEquals(HOUR, schedule.advanceToNextOccurrence(HOUR));
        assertEquals(4000, schedule.advanceToNextOccurrence(4000));

        schedule = new SimulatorProtocol.Schedule(
                LocalDate.of(2000, 1, 1),
                "FREQ=DAILY;"
        );

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();

        assertEquals(0, schedule.advanceToNextOccurrence(now));
        assertEquals(HOUR, schedule.advanceToNextOccurrence(now + HOUR));
        assertEquals(0, schedule.advanceToNextOccurrence(now + DAY));
        assertEquals(0, schedule.advanceToNextOccurrence(now + DAY*2));
        assertEquals(0, schedule.advanceToNextOccurrence(now + DAY*3));
        assertEquals(0, schedule.advanceToNextOccurrence(now + DAY*4));
        assertEquals(1, schedule.advanceToNextOccurrence(now + DAY*4+1));
        assertEquals(DAY-1, schedule.advanceToNextOccurrence(now + DAY*5-1));

        schedule = new SimulatorProtocol.Schedule(
                LocalDate.of(2000, 1, 1),
                "FREQ=DAILY;COUNT=2"
        );
        assertEquals(0, schedule.advanceToNextOccurrence(now));
        assertEquals(HOUR, schedule.advanceToNextOccurrence(now + HOUR));
        assertEquals(0, schedule.advanceToNextOccurrence(now + DAY));
        // Surpassed
        assertEquals(DAY, schedule.advanceToNextOccurrence(now + DAY*2));
    }

    @Test
    public void getDelayForHourlyRecurrence() {
        SimulatorProtocol.Schedule schedule;
        LocalDate start = LocalDate.of(2000, 1, 1);
        try (MockedStatic<LocalDate> mockInstant = Mockito.mockStatic(LocalDate.class)) {
            mockInstant.when(LocalDate::now).thenReturn(start);
            schedule = new SimulatorProtocol.Schedule(
                    null,
                    "FREQ=HOURLY"
            );
        }

        long now = start.toEpochDay() * DAY;
        long timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:39.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(1, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:40.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(3600, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
    }

    @Test
    public void getDelayForHourlyRecurrenceWithStartDate() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(
                LocalDate.of(2000, 1, 2),
                "FREQ=HOURLY;"
        );

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(DAY + 100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(DAY + 40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(DAY - 20, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
    }

    @Test
    public void getDelayCustomRecurringWithEndDate() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(
                LocalDate.of(2000, 1, 1),
                "FREQ=HOURLY;COUNT=2"
        );

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(3580, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.advanceToNextOccurrence(now);
        assertEquals(OptionalLong.empty(), SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule));
    }
}
