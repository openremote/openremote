package org.openremote.agent.simulator;

import mockit.Mock;
import mockit.MockUp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class SimulatorProtocolTest {

    System system;
    ScheduledExecutorService executor;

    @BeforeEach
    void setup() {

//        system = mock(System.class);
        executor = mock(ScheduledExecutorService.class);

//        when(system.nanoTime()).thenReturn(0L);
    }

    @Test
    public void validateSimulatorAgentProtocol() {
//        TimeWrapper wrapper = mock(TimeWrapper.class);
//        when(wrapper.nanoTime()).thenReturn(0L);
        System.out.println(System.nanoTime());
        new MockUp<System>() {
            @Mock
            public long nanoTime(){
                return 1234;
            }
        };
        System.out.println(System.nanoTime());
//        System.out.println(executor.schedule(() -> {}, 0L, TimeUnit.SECONDS).getDelay(TimeUnit.SECONDS));

//        var d = new TimeWrapper();
//        SimulatorProtocol d = new SimulatorProtocol(new SimulatorAgent("test"));
//        d.linkAttribute("test", new Attribute<>());

        assertTrue(System.nanoTime() == 0L);
    }
}
