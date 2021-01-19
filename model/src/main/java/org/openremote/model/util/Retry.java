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
package org.openremote.model.util;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Retry {

    protected String name;
    protected ScheduledExecutorService executorService;
    protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    protected Logger logger;
    protected long initialDelay;
    protected long maxDelay = 5*60000L;
    protected long backoffMultiplier = 2L;
    protected long jitterMargin = 10000L;
    protected int maxRetries = Integer.MAX_VALUE;
    protected boolean running;
    protected int retries;
    Supplier<Boolean> task;
    Future<?> future;
    Runnable successCallback;
    Runnable failureCallback;

    public Retry(String name, ScheduledExecutorService executorService, Supplier<Boolean> task) {
        this.name = name;
        this.executorService = executorService;
        this.task = task;
    }

    public Retry setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public Retry setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public Retry setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public Retry setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
        return this;
    }

    public Retry setBackoffMultiplier(long backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public Retry setJitterMargin(long jitterMargin) {
        this.jitterMargin = jitterMargin;
        return this;
    }

    public Retry setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public Retry setSuccessCallback(Runnable successCallback) {
        this.successCallback = successCallback;
        return this;
    }

    public Retry setFailureCallback(Runnable failureCallback) {
        this.failureCallback = failureCallback;
        return this;
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException(name + ": already running");
        }

        retries = 1;
        running = true;

        if (initialDelay > 0) {
            future = executorService.schedule(this::doAttempt, initialDelay, timeUnit);
        } else {
            future = executorService.submit(this::doAttempt);
        }
    }

    public synchronized void cancel(boolean mayInterrupt) {
        if (!running) {
            return;
        }

        running = false;
        future.cancel(mayInterrupt);
    }

    protected void doAttempt() {
        boolean success = false;
        boolean retry = retries < maxRetries;

        try {
            log(Level.INFO, name + ": running, attempt=" + retries, null);
            success = task.get();
        } catch (Exception e) {
            log(Level.INFO, name + ": threw an exception", e);
        }

        if (!running) {
            return;
        }

        if (success) {
            if (successCallback != null) {
                successCallback.run();
            }
            return;
        }

        if (!retry) {
            if (failureCallback != null) {
                failureCallback.run();
            }
            return;
        }

        long adjustedMax = Math.max(maxDelay - jitterMargin, jitterMargin);
        long delay = Math.max(initialDelay, Math.min(initialDelay * (long)Math.pow(2, retries), adjustedMax));
        delay += Math.random()*jitterMargin;
        retries++;

        log(Level.INFO, name + ": scheduling retry in " + delay + " " + timeUnit.name(), null);
        future = executorService.schedule(this::doAttempt, delay, timeUnit);
    }

    protected void log(Level level, String log, Throwable e) {
        if (logger != null) {
            logger.log(level, log, e);
        }
    }
}
