/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.model.services;

import org.openremote.model.event.shared.RealmScopedEvent;

public class ExternalServiceEvent extends RealmScopedEvent {

  public enum Cause {
    REGISTER,
    UPDATE,
    DEREGISTER
  }

  protected ExternalService service;

  protected ExternalServiceEvent.Cause cause;

  public ExternalServiceEvent(ExternalService service, ExternalServiceEvent.Cause cause) {
    this.realm = service.getRealm();
    this.cause = cause;
    this.service = service;
  }

  public ExternalService getService() {
    return service;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "realm="
        + realm
        + ", service="
        + service
        + ", cause="
        + cause
        + '}';
  }
}
