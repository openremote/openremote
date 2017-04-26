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
package org.openremote.manager.server.concurrent;

import org.openremote.agent3.protocol.ProtocolExecutorService;
import org.openremote.container.Container;
import org.openremote.container.concurrent.ContainerExecutor;
import org.openremote.container.concurrent.ContainerScheduledExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openremote.container.util.MapAccess.getInteger;

public class ManagerExecutorService implements ProtocolExecutorService {

    /**
     * Threads used by rules engines, each rule engine runs in a separate thread and blocks it.
     * The pool size limits the number of rules engines that can exist.
     */
    public static final String RULES_THREADS_MAX = "RULES_THREADS_MAX";
    public static final int RULES_THREADS_MAX_DEFAULT = Integer.MAX_VALUE;

    /**
     * Threads used by scheduled, usually short-lived and non-blocking tasks, such as protocols
     * polling a remote service at regular interval, or the internal checks for expired client
     * subscriptions on the event bus.
     */
    public static final String SCHEDULED_TASKS_THREADS_MAX = "SCHEDULED_TASKS_THREADS_MAX";
    public static final int SCHEDULED_TASKS_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);

    protected ExecutorService rulesExecutor;
    protected ScheduledExecutorService scheduledTasksExecutor;

    @Override
    public void init(Container container) throws Exception {
        int rulesThreadsMax =
            getInteger(container.getConfig(), RULES_THREADS_MAX, RULES_THREADS_MAX_DEFAULT);
        rulesExecutor = new ContainerExecutor("Rules engine", 0, rulesThreadsMax, 60, -1);

        int scheduledTasksThreadsMax =
            getInteger(container.getConfig(), SCHEDULED_TASKS_THREADS_MAX, SCHEDULED_TASKS_THREADS_MAX_DEFAULT);
        scheduledTasksExecutor = new ContainerScheduledExecutor("Scheduled task", scheduledTasksThreadsMax);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
        rulesExecutor.shutdownNow();
        scheduledTasksExecutor.shutdown();
    }

    public ExecutorService getRulesExecutor() {
        return rulesExecutor;
    }

    @Override
    public ScheduledFuture schedule(Runnable runnable, long delayMillis) {
        return scheduledTasksExecutor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture scheduleAtFixedRate(Runnable runnable, long initialDelayMillis, long periodMillis) {
        return scheduledTasksExecutor.scheduleAtFixedRate(runnable, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture scheduleWithFixedDelay(Runnable runnable, long initialDelayMillis, long periodMillis) {
        return scheduledTasksExecutor.scheduleWithFixedDelay(runnable, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
