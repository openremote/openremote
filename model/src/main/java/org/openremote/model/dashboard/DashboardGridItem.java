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
package org.openremote.model.dashboard;

import jakarta.validation.constraints.Min;

public class DashboardGridItem {

  // Fields
  protected String id;
  protected int x;
  protected int y;

  @Min(value = 1, message = "{Dashboard.gridItem.w.Min}") protected int w;

  @Min(value = 1, message = "{Dashboard.gridItem.h.Min}") protected int h;

  protected int minH;
  protected int minW;
  protected int minPixelH;
  protected int minPixelW;
  protected boolean noResize;
  protected boolean noMove;
  protected boolean locked;

  public DashboardGridItem setX(int x) {
    this.x = x;
    return this;
  }

  public DashboardGridItem setY(int y) {
    this.y = y;
    return this;
  }

  public DashboardGridItem setW(int w) {
    this.w = w;
    return this;
  }

  public DashboardGridItem setH(int h) {
    this.h = h;
    return this;
  }
}
