package org.openremote.controller.command.builtin;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.openremote.container.util.TextUtil;
import org.openremote.controller.command.PushCommand;
import org.openremote.controller.model.CommandDefinition;
import org.openremote.controller.model.Sensor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * DateTime commands can be used to display date or time on the console
 * or use certain DateTime events to trigger rules. To calculate sunrise and sunset the location has to be given as longitude, latidude
 * and timezone. Timezone is the string identifier which is available in Java eg. Europe/Berlin
 * <p>
 * The following commands are available in the moment:
 * - date (returns a date/time as string depending on the given formatter)
 * - sunrise (returns the sunrise time as string depending on the given formatter)
 * - sunset (returns the sunset time as string depending on the given formatter)
 * - minutesUntilSunrise (returns an integer with the minutes until sunrise)
 * - minutesUntilSunset (returns an integer with the minutes until sunset)
 * - isDay (returns a boolean string)
 * - isNight (returns a boolean string)
 *
 * @author Marcus
 */
public class DateTimeCommand implements PushCommand, Runnable {

    private static final Logger LOG = Logger.getLogger(DateTimeCommand.class.getName());

    private TimeZone timezone;
    private String command;
    private SimpleDateFormat dateFormatter;
    private SunriseSunsetCalculator calculator;

    /**
     * The polling interval which is used for the sensor update thread
     */
    private Integer pollingInterval;

    private Thread pollingThread;
    private Sensor sensor;

    boolean doPoll = false;


    public DateTimeCommand(CommandDefinition commandDefinition) {
        this(
            commandDefinition.getProperty("latitude"),
            commandDefinition.getProperty("longitude"),
            commandDefinition.getProperty("timezone"),
            commandDefinition.getProperty("command"),
            commandDefinition.getProperty("format"),
            TextUtil.convertPollingIntervalString(commandDefinition.getProperty("pollingInterval", "60000"))
        );
    }

    public DateTimeCommand(String latitude, String longitude, String timezone, String command, String format, Integer pollingInterval) {

        if (command.equals("sunrise") || command.equals("sunset") || command.equals("minutesUntilSunrise")
            || command.equals("minutesUntilSunset") || command.equals("isDay") || command.equals("isNight")) {
            if (null == longitude || null == latitude) {
                throw new IllegalArgumentException("Unable to create DateTime command for sunrise/sunset related command, " +
                    "missing configuration parameter(s)");
            }
        }

        if (timezone == null) {
            this.timezone = TimeZone.getDefault();
        } else {
            this.timezone = TimeZone.getTimeZone(timezone);
        }

        if (format != null) {
            dateFormatter = new SimpleDateFormat(format);
        } else {
            dateFormatter = new SimpleDateFormat();
        }

        dateFormatter.setTimeZone(this.timezone);

        this.command = command;
        this.pollingInterval = pollingInterval;

        if (!command.equalsIgnoreCase("date")) {
            Location location = new Location(latitude, longitude);
            this.calculator = new SunriseSunsetCalculator(location, timezone);
            LOG.fine(
                "DaylightCalculatorCommand created with values latitude=" + latitude + ", longitude=" + longitude + ", timezone=" + timezone
            );
        }
    }

    public String calculateData() {
        if (command.equalsIgnoreCase("date")) {
            return dateFormatter.format(Calendar.getInstance(this.timezone).getTime());
        } else if (command.equalsIgnoreCase("sunrise")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunriseDate = calculator.getOfficialSunriseCalendarForDate(now);
            return dateFormatter.format(officialSunriseDate.getTime());
        } else if (command.equalsIgnoreCase("sunset")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunsetDate = calculator.getOfficialSunsetCalendarForDate(now);
            return dateFormatter.format(officialSunsetDate.getTime());
        } else if (command.equalsIgnoreCase("minutesUntilSunrise")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunriseDate = calculator.getOfficialSunriseCalendarForDate(now);
            if (now.after(officialSunriseDate)) {
                Calendar tomorrow = (Calendar) now.clone();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                officialSunriseDate = calculator.getOfficialSunriseCalendarForDate(tomorrow);
            }
            int daysUntilSunrise = officialSunriseDate.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
            int hoursUntilSunrise = officialSunriseDate.get(Calendar.HOUR_OF_DAY) - now.get(Calendar.HOUR_OF_DAY);
            int minutesUntilSunrise = officialSunriseDate.get(Calendar.MINUTE) - now.get(Calendar.MINUTE);
            return Integer.toString((daysUntilSunrise * 24 * 60) + (hoursUntilSunrise * 60) + minutesUntilSunrise);
        } else if (command.equalsIgnoreCase("minutesUntilSunset")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunsetDate = calculator.getOfficialSunsetCalendarForDate(now);
            if (now.after(officialSunsetDate)) {
                Calendar tomorrow = (Calendar) now.clone();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                officialSunsetDate = calculator.getOfficialSunsetCalendarForDate(tomorrow);
            }
            int daysUntilSunset = officialSunsetDate.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
            int hoursUntilSunset = officialSunsetDate.get(Calendar.HOUR_OF_DAY) - now.get(Calendar.HOUR_OF_DAY);
            int minutesUntilSunset = officialSunsetDate.get(Calendar.MINUTE) - now.get(Calendar.MINUTE);
            return Integer.toString((daysUntilSunset * 24 * 60) + (hoursUntilSunset * 60) + minutesUntilSunset);
        } else if (command.equalsIgnoreCase("isDay")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunriseDate = calculator.getOfficialSunriseCalendarForDate(now);
            Calendar officialSunsetDate = calculator.getOfficialSunsetCalendarForDate(now);
            if (now.after(officialSunriseDate) && now.before(officialSunsetDate)) {
                return "true";
            } else {
                return "false";
            }
        } else if (command.equalsIgnoreCase("isNight")) {
            Calendar now = Calendar.getInstance(timezone);
            Calendar officialSunriseDate = calculator.getOfficialSunriseCalendarForDate(now);
            Calendar officialSunsetDate = calculator.getOfficialSunsetCalendarForDate(now);
            if (now.after(officialSunriseDate) && now.before(officialSunsetDate)) {
                return "false";
            } else {
                return "true";
            }
        }
        LOG.severe("DateTimeCommand does not know about this command: " + command);
        return "Unknown command";
    }

    @Override
    public void start(Sensor sensor) {
        LOG.fine("*** setSensor called as part of EventListener init *** sensor is: " + sensor);
        if (pollingInterval == null) {
            throw new RuntimeException("Could not set sensor because no polling interval was given");
        }
        this.sensor = sensor;
        pollingThread = new Thread(this);
        pollingThread.setName("Polling thread for: " + sensor);
        pollingThread.start();
    }


    @Override
    public void stop(Sensor sensor) {
        this.doPoll = false;
    }

    @Override
    public void run() {
        LOG.fine("Sensor thread started for sensor: " + sensor);
        this.doPoll = true;
        while (this.doPoll) {
            String readValue = this.calculateData();
            if (!"N/A".equals(readValue)) {
                sensor.update(readValue);
            }
            try {
                Thread.sleep(pollingInterval); // We recalculate at requested polling interval
            } catch (InterruptedException e) {
                this.doPoll = false;
                pollingThread.interrupt();
            }
        }
        LOG.fine("*** Out of run method: " + sensor);
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "DateTimeCommand{" +
            "timezone=" + timezone +
            ", command='" + command + '\'' +
            '}';
    }
}
