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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ContainerThreads {

    Logger LOG = Logger.getLogger(ContainerThreads.class.getName());

    RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = new ThreadPoolExecutor.DiscardPolicy() {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            // Log and discard
            LOG.info("Container thread pool '" + threadPoolExecutor + "' rejected execution of " + runnable.getClass());
            super.rejectedExecution(runnable, threadPoolExecutor);
        }
    };

    static Thread startWaitingThread() {
        Thread thread = new Thread("Container Waiting") {
            @Override
            public void run() {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    // Ignore, thrown on shutdown
                }
            }
        };
        thread.setDaemon(false);
        thread.start();
        return thread;
    }

    static void logExceptionCause(Runnable runnable, Throwable throwable) {
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

    static Throwable unwrap(Throwable throwable) throws IllegalArgumentException {
        if (throwable == null) {
            throw new IllegalArgumentException("Cannot unwrap null throwable");
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            throwable = current;
        }
        return throwable;
    }
}
