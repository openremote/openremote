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
package org.openremote.manager.concurrent;

import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.concurrent.ContainerScheduledExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.openremote.container.util.MapAccess.getInteger;

public class ManagerExecutorService implements ProtocolExecutorService {

    /**
     * Threads used by scheduled, usually short-lived and non-blocking tasks, such as protocols
     * polling a remote service at regular interval, or the internal checks for expired client
     * subscriptions on the event bus. Also used by the rules engine.
     */
    public static final String SCHEDULED_TASKS_THREADS_MAX = "SCHEDULED_TASKS_THREADS_MAX";
    public static final int SCHEDULED_TASKS_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    public static final int PRIORITY = ContainerService.HIGH_PRIORITY + 200;
    protected ScheduledExecutorService scheduledTasksExecutor;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        int scheduledTasksThreadsMax =
            getInteger(container.getConfig(), SCHEDULED_TASKS_THREADS_MAX, SCHEDULED_TASKS_THREADS_MAX_DEFAULT);
        scheduledTasksExecutor = new ContainerScheduledExecutor("Scheduled task", scheduledTasksThreadsMax);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
        scheduledTasksExecutor.shutdownNow();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delayMillis) {
        return scheduledTasksExecutor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelayMillis, long periodMillis) {
        return scheduledTasksExecutor.scheduleAtFixedRate(runnable, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelayMillis, long periodMillis) {
        return scheduledTasksExecutor.scheduleWithFixedDelay(runnable, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledTasksExecutor.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduledTasksExecutor.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledTasksExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledTasksExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        // Don't allow protocols to shutdown the executor
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        // Don't allow protocols to shutdown the executor
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return scheduledTasksExecutor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return scheduledTasksExecutor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return scheduledTasksExecutor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return scheduledTasksExecutor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return scheduledTasksExecutor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return scheduledTasksExecutor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return scheduledTasksExecutor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return scheduledTasksExecutor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return scheduledTasksExecutor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return scheduledTasksExecutor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        scheduledTasksExecutor.execute(command);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
