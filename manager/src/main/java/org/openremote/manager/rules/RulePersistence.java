/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RulePersistence {

  private static final Logger LOG = Logger.getLogger(RulePersistence.class.getName());

  // TODO: This should be persistent on disk
  protected final Map<String, String> data = new HashMap<>();

  public void writeData(String key, Object value) {
    LOG.fine("Writing '" + key + "': " + value);
    data.put(key, value != null ? value.toString() : null);
  }

  public String readData(String key) {
    LOG.fine("Reading '" + key + "'");
    return data.get(key);
  }

  public String readData(String key, String defaultValue) {
    LOG.fine("Reading '" + key + "', default: " + defaultValue);
    return data.getOrDefault(key, defaultValue);
  }

  public void deleteData(String key) {
    LOG.fine("Deleting '" + key + "'");
    data.remove(key);
  }
}
