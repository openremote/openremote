/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container;

/**
 * Containers manage a list of services, the order of services in a container is important.
 * <p>
 * Service startup lifecycle:
 * </p>
 * <ol>
 * <li>{@link #init} in insertion order</li>
 * <li>{@link #start} in insertion order</li>
 * </ol>
 * <p>
 * Service shutdown lifecycle:
 * </p>
 * <ol>
 * <li>{@link #stop} in <b>reverse</b> order</li>
 * </ol>
 */
public interface ContainerService {

    /**
     * All services are initialized in the order they have been added to container.
     */
    void init(Container container) throws Exception;

    /**
     * After initialization, services are started in the order they have been added to container.
     */
    void start(Container container) throws Exception;

    /**
     * When the container is shutting down, it stops all services in the reverse order they have been added to container.
     */
    void stop(Container container) throws Exception;

}
