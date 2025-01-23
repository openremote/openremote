package org.openremote.manager.energy.gopacs;

import java.time.*;

public class FlexRequestISPTypeHelper {
    private static final long ISP_DURATION_IN_MINUTES = 15;

    public static LocalTime getISPStart(long ispNumber, int year, int month, int day, String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime date = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zoneId);

        if (ispNumber == 9 && isLastSundayInMarch(year, month, day)) {
            return date.plusHours(3).toLocalTime();
        } else if (ispNumber == 13 && isLastSundayInOctober(year, month, day)) {
            return date.plusHours(2).toLocalTime();
        } else {
            return date.plusMinutes((ispNumber - 1) * ISP_DURATION_IN_MINUTES).toLocalTime();
        }
    }

    public static LocalTime getISPEnd(int ispNumber, int year, int month, int day, String timeZone) {
        LocalTime end = getISPStart(ispNumber, year, month, day, timeZone).plusMinutes(ISP_DURATION_IN_MINUTES);
        if (ispNumber == 8 && isLastSundayInMarch(year, month, day)) {
            end = end.plusHours(1);
        } else if (ispNumber == 12 && isLastSundayInOctober(year, month, day)) {
            end = end.minusHours(1);
        }
        return end;
    }

    private static boolean isLastSundayInMarch(int year, int month, int day) {
        if (month != 3) { // March is 3 in Java's month numbering
            return false;
        }
        LocalDate date = LocalDate.of(year, month, day);
        int lastDayInMarch = YearMonth.of(year, month).lengthOfMonth();
        LocalDate lastSundayInMarch = LocalDate.of(year, month, lastDayInMarch);
        while (lastSundayInMarch.getDayOfWeek() != DayOfWeek.SUNDAY) {
            lastSundayInMarch = lastSundayInMarch.minusDays(1);
        }
        return date.equals(lastSundayInMarch);
    }

    private static boolean isLastSundayInOctober(int year, int month, int day) {
        if (month != 10) { // October is 10 in Java's month numbering
            return false;
        }
        LocalDate date = LocalDate.of(year, month, day);
        int lastDayInOctober = YearMonth.of(year, month).lengthOfMonth();
        LocalDate lastSundayInOctober = LocalDate.of(year, month, lastDayInOctober);
        while (lastSundayInOctober.getDayOfWeek() != DayOfWeek.SUNDAY) {
            lastSundayInOctober = lastSundayInOctober.minusDays(1);
        }
        return date.equals(lastSundayInOctober);
    }
}
