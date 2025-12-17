/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.util;

import java.math.BigDecimal;
import java.util.Map;

/** Convenience functions for accessing {@link Map}. */
public class MapAccess {

  protected MapAccess() {}

  public static String getString(Map<String, String> map, String key, String defaultValue) {
    return map.containsKey(key) ? map.get(key) : defaultValue;
  }

  public static boolean getBoolean(Map<String, String> map, String key, boolean defaultValue) {
    if (map.containsKey(key)) {
      try {
        return Boolean.valueOf(map.get(key));
      } catch (Exception ex) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  public static int getInteger(Map<String, String> map, String key, int defaultValue) {
    if (map.containsKey(key)) {
      try {
        return Integer.valueOf(map.get(key));
      } catch (Exception ex) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  public static BigDecimal getDecimal(
      Map<String, String> map, String key, BigDecimal defaultValue) {
    if (map.containsKey(key)) {
      try {
        return new BigDecimal(map.get(key));
      } catch (Exception ex) {
        return defaultValue;
      }
    }
    return defaultValue;
  }
}
