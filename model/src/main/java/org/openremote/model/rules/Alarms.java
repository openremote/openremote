/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.rules;

import java.util.List;

import org.openremote.model.alarm.Alarm;
import org.openremote.model.util.TsIgnore;

/**
 * Facade for sending {@link Alarm}s from rules RHS.
 */
@TsIgnore
public abstract class Alarms {
    public abstract Long create(Alarm alarm, String userId);
    public abstract void linkAssets(List<String> assetIds, Long alarmId);
}
