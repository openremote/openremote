package org.openremote.agent.protocol.simulator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorAgentProtocolScheduleTest {

    private final static long MINUTE = 60;
    private final static long HOUR = MINUTE * 60;
    private final static long DAY = HOUR * 24;

    @Nested
    public class TryAdvanceActive {

        @Test
        public void shouldReturnStartAsCurrentForever() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, null);

            long[] instants = {
                    Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond(),
                    start.getEpochSecond(),
                    Instant.parse("9999-12-31T23:59:59.999Z").getEpochSecond()
            };
            for (long epoch : instants) {
                assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(epoch));
            }
        }

        @Test
        public void shouldAlwaysReturnStartAsCurrentWithEnd() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z"), end = Instant.parse("2000-01-10T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), LocalDateTime.ofInstant(end, ZoneOffset.UTC), null);

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
                assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(epoch));
            }

            // end will continue to return the start value, so we use another check to end it
            assertTrue(schedule.isAfterScheduleEnd(Instant.parse("2000-01-10T00:00:00.001Z").toEpochMilli()));
            assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(end.getEpochSecond()+1));
        }

        @Test
        public void shouldReturnOnlyCurrentAfterUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;UNTIL=20000105T000000");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(epoch + DAY-1));

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + DAY,
                    start.getEpochSecond() + DAY * 2,
                    start.getEpochSecond() + DAY * 3,
            };
            for (long instant : instants) {
                assertEquals(instant, schedule.tryAdvanceActive(instant + 1));
                // DAY-1 is within the active occurrence
                assertEquals(instant, schedule.tryAdvanceActive(instant + DAY-1));
            }

            // Should only return the start of the current occurrence 2000-01-04T00:00:00.000Z as there are no future
            // occurrences
            long until = Instant.parse("2000-01-04T00:00:00.000Z").getEpochSecond();
            assertEquals(until, schedule.tryAdvanceActive(until));
            assertEquals(until, schedule.tryAdvanceActive(until + DAY));
            assertEquals(until, schedule.tryAdvanceActive(until + DAY + 1));
        }

        @Test
        public void shouldReturnLastOccurrenceAfterCount() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;COUNT=4");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(epoch + DAY-1));

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + DAY,
                    start.getEpochSecond() + DAY * 2,
            };
            for (long instant : instants) {
                assertEquals(instant, schedule.tryAdvanceActive(instant + 1));
                // DAY-1 is within the active occurrence
                assertEquals(instant, schedule.tryAdvanceActive(instant + DAY-1));
            }

            // Should only return the start of the current occurrence 2000-01-04T00:00:00.000Z as there are no future
            // occurrences
            long count = Instant.parse("2000-01-04T00:00:00.000Z").getEpochSecond();
            assertEquals(count, schedule.tryAdvanceActive(count));
            assertEquals(count, schedule.tryAdvanceActive(count + DAY));
            assertEquals(count, schedule.tryAdvanceActive(count + DAY + 1));
        }

        @Test
        public void shouldStartAt1730() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=DAILY;BYHOUR=17;BYMINUTE=30");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond() + 17*HOUR + 30*MINUTE, schedule.tryAdvanceActive(epoch + DAY-1));
        }

        @Test
        public void shouldMinutelyReturnNextUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            // Do not end 'UNTIL' with 'Z' as that would mean UTC, while internally (in ical4j) the next candidate is determined by
            // 'TemporalComparator.INSTANCE.compare' which would be comparing LocalDateTime with OffsetDateTime and thus end differently
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=MINUTELY;UNTIL=20000101T000500");

            long epoch = Instant.parse("1999-01-01T00:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond(), schedule.tryAdvanceActive(epoch + MINUTE-1));

            long[] instants = {
                    start.getEpochSecond(),
                    start.getEpochSecond() + MINUTE,
                    start.getEpochSecond() + MINUTE * 2,
                    start.getEpochSecond() + MINUTE * 3,
            };
            for (long instant : instants) {
                assertEquals(instant, schedule.tryAdvanceActive(instant + 1));
                // DAY-1 is within the active occurrence
                assertEquals(instant, schedule.tryAdvanceActive(instant + MINUTE-1));
            }

            // Should only return the start of the current occurrence 2000-01-01T00:04:00.000Z as there are no future
            // occurrences
            long until = Instant.parse("2000-01-01T00:04:00.000Z").getEpochSecond();
            assertEquals(until, schedule.tryAdvanceActive(until));
            assertEquals(until, schedule.tryAdvanceActive(until + MINUTE));
            assertEquals(until, schedule.tryAdvanceActive(until + MINUTE + 1));
        }

        @Test
        public void shouldCatchUpWithCurrentTime() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=MINUTELY");

            long epoch = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            assertEquals(start.getEpochSecond() + 60*MINUTE, schedule.tryAdvanceActive(epoch));
            assertEquals(start.getEpochSecond() + 61*MINUTE, schedule.tryAdvanceActive(epoch+MINUTE));
        }
    }

    @Nested
    public class GetDelay {

        @Test
        public void getDelayForHourlyRecurrence() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY");

            long now = start.getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:39.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(1, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:40.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(3600, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }

        @Test
        public void getDelayForHourlyRecurrenceWithStartDate() {
            Instant start = Instant.parse("2000-01-02T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(DAY + 100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(DAY + 40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(DAY - 20, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }

        @Test
        public void getDelayCustomRecurringWithUntil() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY;UNTIL=20000101T020000");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            // UNTIL will continue to return positive delays if now < UNTIL + offset, so we use another check to end it
            assertTrue(schedule.isAfterScheduleEnd(Instant.parse("2000-01-01T02:00:00.001Z").toEpochMilli()));

            // The datapoint won't be replayed after the 3rd occurrence so the delay is negative
            now = Instant.parse("2000-01-01T02:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(-20, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T03:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(-3500, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }


        @Test
        public void getDelayCustomRecurringWithCount() {
            Instant start = Instant.parse("2000-01-01T00:00:00.000Z");
            SimulatorProtocol.Schedule schedule = new SimulatorProtocol.Schedule(LocalDateTime.ofInstant(start, ZoneOffset.UTC), null, "FREQ=HOURLY;COUNT=3");

            long now = Instant.parse("2000-01-01T00:00:00.000Z").getEpochSecond();
            long timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T00:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T01:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(3580, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T02:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(100, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T02:01:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(40, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            // The datapoint won't be replayed after the 3rd occurrence so the delay is negative
            now = Instant.parse("2000-01-01T02:02:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(-20, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());

            now = Instant.parse("2000-01-01T03:00:00.000Z").getEpochSecond();
            timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now);
            assertEquals(-3500, SimulatorProtocol.Schedule.getDelay(100, timeSinceOccurrenceStarted, schedule).getAsLong());
        }
    }
}
