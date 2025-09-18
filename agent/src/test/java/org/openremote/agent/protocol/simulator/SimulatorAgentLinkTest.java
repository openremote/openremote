package org.openremote.agent.protocol.simulator;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimulatorAgentLinkTest {

    @Test
    public void getTimeSinceOccurrenceStarted() {
        SimulatorAgentLink.Schedule schedule = new SimulatorAgentLink.Schedule(new Date(3600_000), new Date(3600_000*3), null);

        assertEquals(-3600, schedule.getTimeSinceOccurrenceStarted(0));
        assertEquals(0, schedule.getTimeSinceOccurrenceStarted(3600));
        assertEquals(400, schedule.getTimeSinceOccurrenceStarted(4000));

        schedule = new SimulatorAgentLink.Schedule(
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

        schedule = new SimulatorAgentLink.Schedule(
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

//    @Test
//    public void getDelayDefaultLinkConfig() throws Exception {
//        SimulatorAgentLink link = new SimulatorAgentLink("1");
//        SimulatorAgentLink.Schedule schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
//        assertEquals(100, schedule.getDelay(100));
//        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
//        assertEquals(40, schedule.getDelay(100));
//        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
//        assertEquals(86_380, schedule.getDelay(100));
//    }


    @Test
    public void get() throws Exception {

    }

    @Test
    public void getDelayInfiniteCustomRecurring() throws Exception {
        SimulatorAgentLink agentLink = new SimulatorAgentLink("1");
        SimulatorAgentLink.Schedule schedule = agentLink.setSchedule(
            new SimulatorAgentLink.Schedule(
                Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                null,
                "FREQ=HOURLY;"
        )).schedule;

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(3580, agentLink.getDelay(100, timeSinceOccurrenceStarted));
    }

    @Test
    public void getDelayCustomRecurringWithEndDate() throws Exception {
        SimulatorAgentLink agentLink = new SimulatorAgentLink("1");
        SimulatorAgentLink.Schedule schedule = agentLink.setSchedule(
                new SimulatorAgentLink.Schedule(
                        Date.from(Instant.parse("2000-01-01T00:00:00.000Z")),
                        null,
                        "FREQ=HOURLY;COUNT=2"
                )).schedule;

        long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
        long timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(3580, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(100, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
        timeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertEquals(40, agentLink.getDelay(100, timeSinceOccurrenceStarted));

        now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
        long finalTimeSinceOccurrenceStarted = schedule.getTimeSinceOccurrenceStarted(now);
        assertThrows(Exception.class, () -> agentLink.getDelay(100, finalTimeSinceOccurrenceStarted));
    }
}
