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
package org.openremote.model.rules.flow;

public class Picker {
  private PickerType type;
  private Option[] options;

  public Picker(PickerType type, Option[] options) {
    this.type = type;
    this.options = options;
  }

  public Picker(PickerType type) {
    this.type = type;
    this.options = new Option[0];
  }

  public Picker() {
    type = PickerType.NUMBER;
    options = new Option[] {};
  }

  public PickerType getType() {
    return type;
  }

  public void setType(PickerType type) {
    this.type = type;
  }

  public Option[] getOptions() {
    return options;
  }

  public void setOptions(Option[] options) {
    this.options = options;
  }
}
