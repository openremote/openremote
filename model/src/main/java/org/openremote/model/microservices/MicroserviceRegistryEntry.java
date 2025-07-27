/*
 * Copyright 2025, OpenRemote Inc.
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

/**
 * Internal registry entry that combines microservice data with properties
 * necessary for the microservice registration lifecycle.
 */
public class MicroserviceRegistryEntry {

    private final Microservice microservice;
    private final String instanceId;
    private long expirationTime;
    private final boolean ignoreTTL;

    public MicroserviceRegistryEntry(Microservice microservice, String instanceId, long expirationTime,
            boolean ignoreTTL) {
        this.microservice = microservice;
        this.instanceId = instanceId;
        this.expirationTime = expirationTime;
        this.ignoreTTL = ignoreTTL;
    }

    public Microservice getMicroservice() {
        return microservice;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isIgnoreTTL() {
        return ignoreTTL;
    }

    public boolean isExpired(long currentTime) {
        return !ignoreTTL && expirationTime < currentTime;
    }

    @Override
    public String toString() {
        return "MicroserviceRegistryEntry{" +
                "microservice=" + microservice +
                ", instanceId='" + instanceId + '\'' +
                ", expirationTime=" + expirationTime +
                ", ignoreTTL=" + ignoreTTL +
                '}';
    }
}
