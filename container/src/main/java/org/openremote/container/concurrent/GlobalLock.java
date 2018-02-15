/*
 * Copyright 2018, OpenRemote Inc.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.logging.Level.FINEST;

/**
 * A global reentrant exclusive lock, use convenience methods {@link #withLock} and {@link #withLockReturning}.
 */
public class GlobalLock {

    private static final Logger LOG = Logger.getLogger(GlobalLock.class.getName());

    // Provides exclusive access to shared protocol state
    static protected final ReentrantLock lock = new ReentrantLock(true);

    protected GlobalLock() {
    }

    /**
     * @return Defaults to 6 seconds, should be longer than it takes the router to be enabled/disabled.
     */
    static public int getLockTimeoutMillis() {
        return 6000;
    }

    /**
     * Obtain the lock within {@link #getLockTimeoutMillis()} or throw {@link IllegalStateException}.
     * @param info An informal text that is printed in log messages.
     * @param runnable The guarded code to execute while holding the lock.
     */
    static public void withLock(String info, Runnable runnable) {
        withLockReturning(info, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Obtain the lock within {@link #getLockTimeoutMillis()} or throw {@link IllegalStateException}.
     * @param info An informal text that is printed in log messages.
     * @param supplier The guarded code to execute while holding the lock.
     */
    static public <R> R withLockReturning(String info, Supplier<R> supplier) {
        try {
            if (lock.tryLock(getLockTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                LOG.finest("+ Acquired lock on (count: " + lock.getHoldCount() + "): " + info);
                // LOG.log(FINEST, "+ Acquired lock on (count: " + lock.getHoldCount() + "): " + info, new RuntimeException());
                //LOG.finest("Lock on: " + Arrays.toString(Thread.currentThread().getStackTrace()));
                try {
                    return supplier.get();
                } finally {
                    LOG.finest("- Releasing lock on (count: " + lock.getHoldCount() + "): " + info);
                    if (!lock.isHeldByCurrentThread()) {
                        LOG.severe("Lock is held by another thread");
                    } else {
                        lock.unlock();
                    }
                }
            } else {
                throw new IllegalStateException(
                    "Could not acquire lock after waiting " + getLockTimeoutMillis() + "ms on: " + info
                );
            }
        } catch (InterruptedException ex) {
            LOG.log(FINEST, "Interrupted while waiting for lock on: " + info);
            return null;
        }
    }
}