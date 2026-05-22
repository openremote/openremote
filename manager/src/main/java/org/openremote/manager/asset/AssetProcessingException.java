/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.asset;

import org.openremote.model.attribute.AttributeWriteFailure;

/** The reason why processing an {@link org.openremote.model.attribute.AttributeEvent} failed. */
public class AssetProcessingException extends RuntimeException {

  protected final AttributeWriteFailure reason;

  public AssetProcessingException(AttributeWriteFailure reason) {
    this(reason, null);
  }

  public AssetProcessingException(AttributeWriteFailure reason, String message) {
    this(reason, message, null);
  }

  public AssetProcessingException(AttributeWriteFailure reason, String message, Throwable cause) {
    super(reason + (message != null ? " (" + message + ")" : ""), cause);
    this.reason = reason;
  }

  public AttributeWriteFailure getReason() {
    return reason;
  }
}
