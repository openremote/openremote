package org.openremote.agent.protocol.simulator;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
