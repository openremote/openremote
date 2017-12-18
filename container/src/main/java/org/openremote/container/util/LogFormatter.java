/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.util;

import org.openremote.model.util.TextUtil;

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

        StringBuilder sb = new StringBuilder(250);

        sb.append(TextUtil.pad(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")), 24));
        sb.append(" ");
        sb.append(TextUtil.pad(record.getLevel().toString(), 7));
        sb.append(" ");
        sb.append("[").append(
            TextUtil.pad(Thread.currentThread().getName().replaceFirst("(.{24}).+(.{4})", "$1..$2"), 30)
        ).append("] ");
        sb.append(TextUtil.pad(TextUtil.truncate(record.getLoggerName(), 40, true), 40));
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


}
