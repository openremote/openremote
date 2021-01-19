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
package org.openremote.agent.protocol.timer;

// TODO: Add support for other links (e.g. DAY_OF_WEEK)
public enum TimerValue {

    /**
     * Links an attribute to the enabled status of a timer for read/write
     */
    ACTIVE,

    /**
     * Links the entire cron expression to an attribute for read/write
     */
    CRON_EXPRESSION,

    /**
     * Links the time of the cron expression to an attribute for read/write
     * <p>
     * Read works for simple hour, min and second cron expressions (e.g. 0 35 15 ? * *) once a write is performed to a
     * non simple cron expression then it becomes simple.
     *
     * 24h time format
     */
    TIME
}
