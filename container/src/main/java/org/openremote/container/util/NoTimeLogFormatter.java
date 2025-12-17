/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.openremote.model.util.TextUtil;

/** A simple log formatter that doesn't output time (assumes time is handled by logging consumer) */
public class NoTimeLogFormatter extends Formatter {

  public String format(LogRecord record) {
    StringBuilder sb = new StringBuilder(250);

    sb.append(TextUtil.pad(record.getLevel().toString(), 7));
    sb.append(" ");
    sb.append("[")
        .append(
            TextUtil.pad(
                Thread.currentThread().getName().replaceFirst("(.{24}).+(.{4})", "$1..$2"), 30))
        .append("] ");
    sb.append(TextUtil.pad(TextUtil.truncate(record.getLoggerName(), 40, true), 40));
    sb.append(" : ");
    sb.append(formatMessage(record));

    sb.append("\n");

    Throwable throwable = record.getThrown();
    if (throwable != null) {
      StringWriter sink = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sink, true));
      sb.append(sink);
    }

    return sb.toString();
  }
}
