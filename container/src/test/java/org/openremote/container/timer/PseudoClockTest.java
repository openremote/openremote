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
package org.openremote.container.timer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PseudoClockTest {

  private TimerService.Clock clock;

  @BeforeEach
  public void setup() {
    clock = TimerService.Clock.PSEUDO;
    clock.init();
    clock.stop();
  }

  @Test
  public void testSetTime() {
    clock.setTime(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneId.of("UTC"));
    assertEquals(0, clock.getCurrentTimeMillis());

    clock.setTime(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0), ZoneId.of("UTC"));
    assertEquals(24 * 3_600_000, clock.getCurrentTimeMillis());

    clock.setTime(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0), ZoneId.of("CET"));
    assertEquals(-3_600_000, clock.getCurrentTimeMillis());

    clock.setTime(LocalDate.of(1970, 1, 2), LocalTime.of(0, 0), ZoneId.of("CET"));
    assertEquals(23 * 3_600_000, clock.getCurrentTimeMillis());
  }

  @Test
  public void testSetTimeISO() {
    clock.setTime("1970-01-01T00:00:00.000Z");
    assertEquals(0, clock.getCurrentTimeMillis());

    clock.setTime("1970-01-02T00:00:00.000Z");
    assertEquals(24 * 3_600_000, clock.getCurrentTimeMillis());

    clock.setTime("1970-01-01T00:00:00.000+01:00");
    assertEquals(-3_600_000, clock.getCurrentTimeMillis());

    clock.setTime("1970-01-02T00:00:00.000+01:00");
    assertEquals(23 * 3_600_000, clock.getCurrentTimeMillis());
  }
}
