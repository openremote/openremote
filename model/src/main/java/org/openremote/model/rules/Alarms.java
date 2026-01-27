/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.rules;

import java.util.List;

import org.openremote.model.alarm.Alarm;
import org.openremote.model.util.TsIgnore;

/** Facade for sending {@link Alarm}s from rules RHS. */
@TsIgnore
public abstract class Alarms {
  public abstract Long create(Alarm alarm, List<String> assetIds);

  public abstract void linkAssets(List<String> assetIds, Long alarmId);
}
