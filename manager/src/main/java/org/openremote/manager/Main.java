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
package org.openremote.manager;

import org.openremote.container.Container;
import org.openremote.container.util.LogUtil;

public class Main {

  static {
    LogUtil.initialiseJUL();
  }

  public static void main(String[] args) throws Exception {
    Container container = null;

    try {
      container = new Container();
      container.startBackground();
    } catch (Exception e) {
      if (container != null) {
        container.stop();
      }
      System.exit(1);
    }
  }
}
