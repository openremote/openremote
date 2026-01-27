/*
 * Copyright 2018, OpenRemote Inc.
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.openremote.model.event.Event;
import org.openremote.model.util.TsIgnore;

/** A rule fact that has a timestamp, it expires after a certain time. */
@TsIgnore
public class TemporaryFact<T> extends Event {

  protected final long expirationMilliseconds;
  protected final T fact;
  // These are only useful for debugging, easier to read than timestamps
  public final LocalDateTime time;
  public final LocalDateTime expirationTime;

  public TemporaryFact(long timestamp, long expirationMilliseconds, T fact) {
    super(timestamp);
    this.expirationMilliseconds = expirationMilliseconds;
    this.fact = fact;
    this.time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    this.expirationTime = time.plus(expirationMilliseconds, ChronoUnit.MILLIS);
  }

  public long getExpirationMilliseconds() {
    return expirationMilliseconds;
  }

  public T getFact() {
    return fact;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public LocalDateTime getExpirationTime() {
    return expirationTime;
  }

  public boolean isExpired(long currentTimestamp) {
    return getTimestamp() + getExpirationMilliseconds() < currentTimestamp;
  }

  @Override
  public String toString() {
    return TemporaryFact.class.getSimpleName()
        + "{"
        + "timestamp="
        + timestamp
        + ", time="
        + time
        + ", expirationMilliseconds="
        + expirationMilliseconds
        + ", expirationTime="
        + expirationTime
        + ", fact="
        + fact
        + '}';
  }
}
