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

import java.util.Arrays;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DashboardTemplate {

  // Fields
  protected String id;

  @Min(value = 1, message = "${Dashboard.template.columns.Min}") protected int columns;

  @Min(value = 1, message = "${Dashboard.template.maxScreenWidth.Min}") protected int maxScreenWidth;

  @Valid protected DashboardRefreshInterval refreshInterval;

  @NotNull(message = "{Dashboard.template.screenPresets.NotNull}") @Valid protected DashboardScreenPreset[] screenPresets;

  @Valid protected DashboardWidget[] widgets;

  /* -------------------- */

  public DashboardTemplate() {}

  public DashboardTemplate(DashboardScreenPreset[] screenPresets) {
    this.columns = 1;
    this.maxScreenWidth = 1;
    this.refreshInterval = DashboardRefreshInterval.OFF;
    this.screenPresets = screenPresets;
  }

  public DashboardTemplate setId(String id) {
    this.id = id;
    return this;
  }

  public DashboardTemplate setColumns(int columns) {
    this.columns = columns;
    return this;
  }

  public DashboardTemplate setMaxScreenWidth(int maxScreenWidth) {
    this.maxScreenWidth = maxScreenWidth;
    return this;
  }

  public DashboardTemplate setRefreshInterval(DashboardRefreshInterval interval) {
    this.refreshInterval = interval;
    return this;
  }

  public DashboardTemplate setScreenPresets(DashboardScreenPreset[] screenPresets) {
    this.screenPresets = screenPresets;
    return this;
  }

  public DashboardTemplate setWidgets(DashboardWidget[] widgets) {
    this.widgets = widgets;
    return this;
  }

  public String getId() {
    return id;
  }

  public int getColumns() {
    return columns;
  }

  public int getMaxScreenWidth() {
    return maxScreenWidth;
  }

  public DashboardRefreshInterval getRefreshInterval() {
    return this.refreshInterval;
  }

  public DashboardScreenPreset[] getScreenPresets() {
    return screenPresets;
  }

  public DashboardWidget[] getWidgets() {
    return widgets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DashboardTemplate that)) return false;
    return columns == that.columns
        && maxScreenWidth == that.maxScreenWidth
        && Objects.equals(id, that.id)
        && refreshInterval == that.refreshInterval
        && Objects.deepEquals(screenPresets, that.screenPresets)
        && Objects.deepEquals(widgets, that.widgets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        columns,
        maxScreenWidth,
        refreshInterval,
        Arrays.hashCode(screenPresets),
        Arrays.hashCode(widgets));
  }
}
