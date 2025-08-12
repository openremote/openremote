/*
 * Copyright 2024, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.microservices;

import org.openremote.model.event.shared.RealmScopedEvent;

public class MicroserviceEvent extends RealmScopedEvent {

    public enum Cause {
        REGISTER, UPDATE, DEREGISTER
    }

    protected Microservice microservice;

    protected MicroserviceEvent.Cause cause;

    public MicroserviceEvent(Microservice microservice, MicroserviceEvent.Cause cause) {
        this.realm = microservice.getRealm();
        this.cause = cause;
        this.microservice = microservice;
    }

    public Microservice getMicroservice() {
        return microservice;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "realm=" + realm +
                ", microservice=" + microservice +
                ", cause=" + cause +
                '}';
    }
}
