package org.openremote.agent.rules;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RuleUtil {

    /**
     * @param o A timestamp string as 'HH:mm:ss' or 'HH:mm'.
     * @return Epoch time or 0 if there is a problem parsing the timestamp string.
     */
    public long parseTimestamp(Object o) {
        String timestamp = o.toString();
        SimpleDateFormat sdf;
        if (timestamp.length() == 8) {
            sdf = new SimpleDateFormat("HH:mm:ss");
        } else if (timestamp.length() == 5) {
            sdf = new SimpleDateFormat("HH:mm");
        } else {
            return (0L);
        }
        try {
            return (sdf.parse(timestamp).getTime());
        } catch (ParseException e) {
            return (0L);
        }
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm' or <code>null</code> if the timestamp is <= 0.
     */
    public String formatTimestamp(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm:ss' or <code>null</code> if the timestamp is <= 0.
     */
    public String formatTimestampWithSeconds(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param o       A timestamp string as 'HH:mm' or '-'.
     * @param minutes The minutes to increment/decrement from timestamp.
     * @return Timestamp string as 'HH:mm', modified with the given minutes or the current time + 60 minutes if
     * the given timestamp was '-' or the given timestamp couldn't be parsed.
     */
    public String shiftTime(Object o, int minutes) {
        String timestamp = o.toString();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = null;
        if (timestamp != null && timestamp.length() >= 1 && timestamp.substring(0, 1).equals("-")) {
            date = new Date();
            date.setTime(date.getTime() + 60 * 60000);
        } else {
            try {
                date = sdf.parse(timestamp);
                date.setTime(date.getTime() + minutes * 60000);
            } catch (ParseException ex) {
                date = new Date();
                date.setTime(date.getTime() + 60 * 60000);
            }
        }
        return (sdf.format(date));
    }

    /**
     * @param o A string representation of a double value.
     * @return The parsed value or 0.0 if the string couldn't be parsed.
     */
    public Double parseDouble(Object o) {
        String s = o.toString();
        try {
            if (s.length() >= 1) {
                return (Double.parseDouble(s.substring(0, s.length() - 1)));
            } else {
                return (0.0);
            }
        } catch (NumberFormatException e) {
            return (0.0);
        }
    }

    /**
     * @param o A string representation of a double value.
     * @param shift Increments or decrements the parsed value.
     * @param suffix A string appended to the result.
     */
    public String shiftDouble(Object o, double shift, String suffix) {
        Double d = parseDouble(o);
        d = d + shift;
        return (String.format("%.1f", d) + suffix);
    }
}
