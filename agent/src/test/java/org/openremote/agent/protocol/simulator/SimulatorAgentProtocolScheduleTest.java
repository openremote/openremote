package org.openremote.agent.protocol.simulator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulatorAgentProtocolScheduleTest {

    private final static long MINUTE = 60;
    private final static long HOUR = MINUTE * 60;
    private final static long DAY = HOUR * 24;

    @Nested
    public class TryAdvanceActive {

        @Test
        public void shouldReturnSomeForever() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, null);

            long[] instants = {
                    Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond(),
                    start.getEpochSecond(),
                    Instant.parse("9999-12-31T23:59:59.999Z").getEpochSecond()
            };
            for (long epoch : instants) {
                assertEquals(start.getEpochSecond(), event.tryAdvanceActive(epoch).getAsLong());
            }
        }

        @Test
        public void shouldReturnEmptyAt() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z"), end = Instant.parse("2000-01-10T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), LocalDateTime.ofInstant(end, ZoneOffset.UTC), null);

            long[] instants = {
                    Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond(),
                    start.getEpochSecond(),
                    start.getEpochSecond() + DAY,
                    start.getEpochSecond() + DAY * 2,
                    start.getEpochSecond() + DAY * 3,
                    start.getEpochSecond() + DAY * 4,
                    start.getEpochSecond() + DAY * 5,
                    start.getEpochSecond() + DAY * 6,
                    start.getEpochSecond() + DAY * 7,
                    start.getEpochSecond() + DAY * 8,
                    start.getEpochSecond() + DAY * 9,
            };
            for (long epoch : instants) {
                assertEquals(start.getEpochSecond(), event.tryAdvanceActive(epoch).getAsLong());
            }
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end.getEpochSecond()+1));
        }

        @Test
        public void shouldReturnSomeUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;UNTIL=20000105T000000");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), event.tryAdvanceActive(epoch + DAY-1).getAsLong());

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + DAY,
                    start.getEpochSecond() + DAY * 2,
                    start.getEpochSecond() + DAY * 3,
            };
            for (long instant : instants) {
                assertEquals(instant, event.tryAdvanceActive(instant + 1).getAsLong());
                // DAY-1 is within the active occurrence
                assertEquals(instant, event.tryAdvanceActive(instant + DAY-1).getAsLong());
            }

            long end = Instant.parse("2000-01-05T00:00:00.000Z").getEpochSecond();
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end + 1));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end + DAY));
        }

        @Test
        public void shouldReturnNoneAfter() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;COUNT=4");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), event.tryAdvanceActive(epoch + DAY-1).getAsLong());

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + DAY,
                    start.getEpochSecond() + DAY * 2,
            };
            for (long instant : instants) {
                assertEquals(instant, event.tryAdvanceActive(instant + 1).getAsLong());
                // DAY-1 is within the active occurrence
                assertEquals(instant, event.tryAdvanceActive(instant + DAY-1).getAsLong());
            }
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(start.getEpochSecond() + DAY * 3));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(start.getEpochSecond() + DAY * 3 + 1));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(start.getEpochSecond() + DAY * 4));
        }

        @Test
        public void shouldStartAt1730() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;BYHOUR=17;BYMINUTE=30");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond() + 17*HOUR + 30*MINUTE, event.tryAdvanceActive(epoch + DAY-1).getAsLong());
        }

        @Test
        public void shouldMinutelyReturnSomeUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            // Do not end 'UNTIL' with 'Z' as that would mean UTC, while internally (in ical4j) the next candidate is determined by
            // 'TemporalComparator.INSTANCE.compare' which would be comparing LocalDateTime with OffsetDateTime and thus end differently
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=MINUTELY;UNTIL=20000101T000500");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), event.tryAdvanceActive(epoch + MINUTE-1).getAsLong());

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + MINUTE,
                    start.getEpochSecond() + MINUTE * 2,
                    start.getEpochSecond() + MINUTE * 3,
            };
            for (long instant : instants) {
                assertEquals(instant, event.tryAdvanceActive(instant + 1).getAsLong());
                // DAY-1 is within the active occurrence
                assertEquals(instant, event.tryAdvanceActive(instant + MINUTE-1).getAsLong());
            }

            long end = Instant.parse("2000-01-01T00:05:00.000Z").getEpochSecond();
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end + 1));
            assertEquals(OptionalLong.empty(), event.tryAdvanceActive(end + MINUTE));
        }

        @Test
        public void shouldCatchUpWithCurrentTime() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule event = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=MINUTELY");

            long epoch = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond() + 60*MINUTE, event.tryAdvanceActive(epoch).getAsLong());
            assertEquals(start.getEpochSecond() + 61*MINUTE, event.tryAdvanceActive(epoch+MINUTE).getAsLong());
        }
    }


    @Nested
    public class GetDelay {

        @Test
        public void getDelayForHourlyRecurrence() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY");

            long now = start.getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:39.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(1, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:40.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(3600, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }

        @Test
        public void getDelayForHourlyRecurrenceWithStartDate() {
            Instant start = Instant.parse("2000-01-02T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(DAY + 100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(DAY + 40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(DAY - 20, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }

        @Test
        public void getDelayCustomRecurringWithUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY;UNTIL=20000101T020000Z");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T02:00:00.000Z").getEpochSecond();
            assertEquals(OptionalLong.empty(), schedule.tryAdvanceActive(now));
        }

        @Test
        public void getDelayCustomRecurringWithCount() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY;COUNT=3");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now).getAsLong();
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T02:00:00.000Z").getEpochSecond();
            assertEquals(OptionalLong.empty(), schedule.tryAdvanceActive(now));
        }
    }
}
