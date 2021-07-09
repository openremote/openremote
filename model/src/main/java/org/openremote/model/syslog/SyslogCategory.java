/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.syslog;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public enum SyslogCategory {

    ASSET(
        "Asset Processing",
        false,
        "AssetProcessingService",
        "AssetStorageService",
        "AssetDatapointService"
    ),
    AGENT(
        "Agents",
        false,
        "AgentService"
    ),
    NOTIFICATION(
        "Notification",
        false,
        "NotificationService",
        "FCMDeliveryService"
    ),
    RULES(
        "Rules",
        true,
        "RulesService",
        "RulesEngine",
        "RuleExecutionLogger",
        "Rules",
        "RulesEngineStats",
        "RulesFired"
    ),
    PROTOCOL(
        "Protocol",
        true,
        "Protocol"
    ),
    GATEWAY(
        "Gateway",
        true,
        "GatewayService",
        "GatewayConnector"
    ),
    MODEL_AND_VALUES(
        "Model and Value",
        true
    ),
    API(
            "API",
            true
    ),
    DATA(
            "DATA",
            true
    );

    protected final String categoryLabel;
    protected final boolean includeSubCategory;
    protected final String[] mappedLoggerNames;

    SyslogCategory(String categoryLabel, boolean includeSubCategory, String... mappedLoggerNames) {
        this.categoryLabel = categoryLabel;
        this.includeSubCategory = includeSubCategory;
        this.mappedLoggerNames = mappedLoggerNames;
    }

    protected String getLoggerName(LogRecord logRecord) {
        boolean haveMatch = false;
        String loggerName = logRecord.getLoggerName();
        if (loggerName.endsWith(name())) {
            loggerName = loggerName.substring(0, loggerName.length() - name().length());
            haveMatch = true;
        }
        if (!haveMatch) {
            for (String mappedLoggerName : mappedLoggerNames) {
                if (loggerName.endsWith(mappedLoggerName)) {
                    haveMatch = true;
                    loggerName = loggerName.substring(0, loggerName.length() - mappedLoggerName.length());
                    break;
                }
            }
        }
        return haveMatch ? loggerName : null;
    }

    protected String getSubCategory(String loggerName) {
        if (!includeSubCategory)
            return null;
        // Sub category is the last word of the logger name (separated by dots)
        String[] words = loggerName.split("\\.");
        if (words.length > 0) {
            return words[words.length-1];
        }
        return null;
    }

    public static SyslogEvent mapSyslogEvent(LogRecord record) {
        SyslogLevel level = SyslogLevel.getLevel(record.getLevel().intValue());
        if (level == null)
            return null;

        // Only log org.openremote records!
        if (!record.getLoggerName().startsWith("org.openremote"))
            return null;

        for (SyslogCategory category : values()) {
            String loggerName = category.getLoggerName(record);
            if (loggerName != null) {

                String subCategory = category.getSubCategory(loggerName);

                // Append the exception message if this is a severe error
                String message = level.ordinal() >= SyslogLevel.ERROR.ordinal() && record.getThrown() != null
                    ? record.getMessage() + " -- " + record.getThrown().getMessage()
                    : record.getMessage();

                return new SyslogEvent(record.getMillis(), level, category, subCategory, message);
            }
        }
        return null;
    }

    public static Logger getLogger(SyslogCategory category, Class<?> loggerName) {
        return getLogger(category, loggerName.getName());
    }

    public static Logger getLogger(SyslogCategory category, String loggerName) {
        return Logger.getLogger((loggerName + "." + category.name()));
    }
}
