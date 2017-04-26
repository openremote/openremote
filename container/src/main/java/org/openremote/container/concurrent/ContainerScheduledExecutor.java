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
package org.openremote.container.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static org.openremote.container.concurrent.ContainerThreads.DEFAULT_REJECTED_EXECUTION_HANDLER;
import static org.openremote.container.concurrent.ContainerThreads.logExceptionCause;

public class ContainerScheduledExecutor extends ScheduledThreadPoolExecutor {

    public ContainerScheduledExecutor(String name, int corePoolSize) {
        this(new ContainerThreadFactory(name), DEFAULT_REJECTED_EXECUTION_HANDLER, corePoolSize);
    }

    public ContainerScheduledExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler, int corePoolSize) {
        super(corePoolSize, threadFactory, rejectedHandler);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        logExceptionCause(runnable, throwable);
    }

}