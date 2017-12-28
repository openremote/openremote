/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol;

import org.openremote.container.ContainerService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * All protocol implementors should use this service to execute non-blocking background tasks.
 * <p>
 * TODO: If we need to run blocking tasks in protocols we can add a regular thread pool/ExecutorService here
 */
public interface ProtocolExecutorService extends ScheduledExecutorService, ContainerService {

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule
     */
    ScheduledFuture schedule(Runnable runnable, long delayMillis);

    /**
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate)
     */
    ScheduledFuture scheduleAtFixedRate(Runnable runnable, long initialDelayMillis, long periodMillis);

    /**
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay
     */
    ScheduledFuture scheduleWithFixedDelay(Runnable runnable, long initialDelayMillis, long periodMillis);
}
