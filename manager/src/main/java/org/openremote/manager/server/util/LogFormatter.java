package org.openremote.manager.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    public String format(LogRecord record) {
        OffsetDateTime date = fromMillis(record.getMillis());

        StringBuilder sb = new StringBuilder(180);

        sb.append(pad(date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), 29));
        sb.append(" ");
        sb.append(pad(record.getLevel().toString(), 7));
        sb.append(" ");
        sb.append("[").append(pad(truncate(Thread.currentThread().getName(), 20, true), 20)).append("] ");
        sb.append(pad(truncate(record.getLoggerName(), 40, true), 40));
        sb.append(" : ");
        sb.append(formatMessage(record));

        sb.append("\n");

        Throwable throwable = record.getThrown();
        if (throwable != null) {
            StringWriter sink = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sink, true));
            sb.append(sink.toString());
        }

        return sb.toString();
    }

    protected OffsetDateTime fromMillis(long epochMillis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    protected String pad(String s, int size) {
        while (s.length() < size) {
            s = s + " ";
        }
        return s;
    }

    protected String truncate(String s, int maxLength, boolean alignRight) {
        if (s.length() <= maxLength)
            return s;

        return alignRight
            ? s.substring(s.length() - maxLength, s.length())
            : s.substring(s.length() - maxLength);
    }
}
