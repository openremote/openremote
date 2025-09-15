package org.openremote.agent.protocol.simulator;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.junit.jupiter.api.Test;
import org.openremote.model.util.TimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimulatorAgentLinkTest {
    @Test
    public void getDelayDefaultLinkConfig() throws Exception {
        SimulatorAgentLink link = new SimulatorAgentLink("1");
        SimulatorAgentLink.Schedule schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(100, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(40, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(86_380, schedule.getDelay(100));
    }

    @Test
    public void getDelayInfiniteCustomRecurring() throws Exception {
        SimulatorAgentLink link = new SimulatorAgentLink("1");
        link.setDuration(new TimeUtil.ExtendedPeriodAndDuration("PT20M"));
        link.setRecurrence(new Recur<LocalDateTime>(Frequency.HOURLY));
        SimulatorAgentLink.Schedule schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(100, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(40, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(3580, schedule.getDelay(100));
    }

    @Test
    public void getDelayCustomRecurringWithEndDate() throws Exception {
        SimulatorAgentLink link = new SimulatorAgentLink("1");
        link.setDuration(new TimeUtil.ExtendedPeriodAndDuration("PT20M"));
        link.setRecurrence(new Recur<LocalDateTime>("FREQ=HOURLY;COUNT=2"));
        SimulatorAgentLink.Schedule schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(100, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(40, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 0, 2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(3580, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 1, 0).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(100, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 1, 1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        assertEquals(40, schedule.getDelay(100));
        schedule = link.getSchedule(LocalDateTime.of(2000, 1, 1, 1, 2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.of(2000, 1, 1, 0, 0));
        SimulatorAgentLink.Schedule finalSchedule = schedule;
        assertThrows(Exception.class, () ->  {
            finalSchedule.getDelay(100);
        });
    }
}
