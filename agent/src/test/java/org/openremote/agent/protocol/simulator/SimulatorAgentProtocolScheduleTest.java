package org.openremote.agent.protocol.simulator;

import net.fortuna.ical4j.model.Recur;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimulatorAgentProtocolScheduleTest {

    @Test
    public void getTimeSinceOccurrenceStarted() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(new Date(3600_000), new Date(3600_000*3), null);

        assertEquals(-3600, schedule.getTimeSinceOccurrenceStarted(0));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(3600));
        assertEquals(400, schedule.getTimeSinceOccurrenceStarted(4000));

        schedule = new SimulatorProtocol.Schedule(
                Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                null,
                "FREQ=DAILY;"
        );

        var now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();

        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now));
        assertEquals(3600, schedule.getTimeSinceOccurrenceStarted(now + 3600));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now + 86400));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now + 86400*2));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now + 86400*3));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now + 86400*4));
        assertEquals(1, schedule.getTimeSinceOccurrenceStarted(now + 86400*4+1));
        assertEquals(86400-1, schedule.getTimeSinceOccurrenceStarted(now + 86400*5-1));

        schedule = new SimulatorProtocol.Schedule(
                Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                null,
                "FREQ=DAILY;COUNT=2"
        );
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now));
        assertEquals(3600, schedule.getTimeSinceOccurrenceStarted(now + 3600));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(now + 86400));
        // Surpassed
        assertEquals(86400, schedule.getTimeSinceOccurrenceStarted(now + 86400*2));
    }

    @Test
    public void getDelayInfiniteCustomRecurring() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(
                Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                null,
                "FREQ=HOURLY;"
        );

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(3580, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
    }

    @Test
    public void getDelayCustomRecurringWithEndDate() {
        SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(
                Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                null,
                "FREQ=HOURLY;COUNT=2"
        );

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(3580, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

        now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(OptionalLong.empty(), SimulatorProtocol.getDelay(100, timeSinceOccurrenceStarted, schedule));
    }
}
