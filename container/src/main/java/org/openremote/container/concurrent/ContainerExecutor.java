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

import static org.openremote.container.concurrent.ContainerThreads.DEFAULT_REJECTED_EXECUTION_HANDLER;
import static org.openremote.container.concurrent.ContainerThreads.logExceptionCause;

public class ContainerExecutor extends ThreadPoolExecutor {

    /**
     * Creates an unbounded thread pool with {@link SynchronousQueue}, this is the same as
     * {@link Executors#newCachedThreadPool}.
     */
    public ContainerExecutor(String name) {
        this(name, 0, Integer.MAX_VALUE, 60L, -1);
    }

    /**
     * @param blockingQueueCapacity Set to <code>-1</code> if a {@link SynchronousQueue} should be used.
     */
    public ContainerExecutor(String name,
                             int corePoolSize,
                             int maximumPoolSize,
                             long keepAliveSeconds,
                             int blockingQueueCapacity) {
        this(
            new ContainerThreadFactory(name),
            DEFAULT_REJECTED_EXECUTION_HANDLER,
            corePoolSize,
            maximumPoolSize,
            keepAliveSeconds,
            blockingQueueCapacity == -1 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(blockingQueueCapacity)
        );
    }

    public ContainerExecutor(ThreadFactory threadFactory,
                             RejectedExecutionHandler rejectedHandler,
                             int corePoolSize,
                             int maximumPoolSize,
                             long keepAliveSeconds,
                             BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveSeconds, TimeUnit.SECONDS, workQueue, threadFactory, rejectedHandler);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        logExceptionCause(runnable, throwable);
    }
}