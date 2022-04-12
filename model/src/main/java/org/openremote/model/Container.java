/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model;

import org.openremote.model.util.TsIgnore;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

@TsIgnore
public interface Container {

    String OR_DEV_MODE = "OR_DEV_MODE";
    boolean OR_DEV_MODE_DEFAULT = true;

    boolean isDevMode();

    Map<String, String> getConfig();

    ContainerService[] getServices();

    ScheduledExecutorService getExecutorService();

    <T extends ContainerService> Collection<T> getServices(Class<T> type);

    <T extends ContainerService> T getService(Class<T> type) throws IllegalStateException;

    <T extends ContainerService> boolean hasService(Class<T> type);
}
