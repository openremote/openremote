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

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread pool that adds logging for tasks that throw exceptions
 */
public class ContainerExecutor extends ThreadPoolExecutor {

    protected static final Logger LOG = Logger.getLogger(ContainerExecutor.class.getName());
    protected String name;

    /**
     * @param blockingQueueCapacity Set to <code>-1</code> if a {@link SynchronousQueue} should be used.
     */
    public ContainerExecutor(String name,
                             int corePoolSize,
                             int maximumPoolSize,
                             long keepAliveSeconds,
                             int blockingQueueCapacity,
                             RejectedExecutionHandler rejectedExecutionHandler) {
        super(
            corePoolSize,
            maximumPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            blockingQueueCapacity == -1 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(blockingQueueCapacity),
            new ContainerThreadFactory(name),
            // Wrap rejected handler to add logging
            (r, executor) -> {
                // Log and discard
                LOG.info("Container thread pool '" + executor + "' rejected execution of " + r);
                rejectedExecutionHandler.rejectedExecution(r, executor);
            }
        );

        this.name = name;
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        logExceptionCause(runnable, throwable);
    }

    protected static void logExceptionCause(Runnable runnable, Throwable throwable) {
        if (throwable != null) {
            Throwable cause = unwrap(throwable);
            if (cause instanceof InterruptedException) {
                // Ignore this, might happen when we shutdownNow() the executor. We can't
                // log at this point as the logging system might be stopped already.
                return;
            }
            LOG.log(Level.WARNING, "Thread terminated unexpectedly executing: " +runnable.getClass(), throwable);
        }
    }

    protected static Throwable unwrap(Throwable throwable) throws IllegalArgumentException {
        if (throwable == null) {
            throw new IllegalArgumentException("Cannot unwrap null throwable");
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            throwable = current;
        }
        return throwable;
    }

    @Override
    public String toString() {
        return name + ":" + super.toString();
    }
}
