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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * A global reentrant exclusive lock, use convenience methods {@link #withLock} and {@link #withLockReturning}.
 */
public class GlobalLock {

    protected static final int LOCK_WARNING_MILLIS = 10000;
    private static final Logger LOG = Logger.getLogger(GlobalLock.class.getName());

    /**
     * At least getOwner() is protected and not private...
     */
    static class CustomReentrantLock extends ReentrantLock {

        String info;

        public CustomReentrantLock() {
            super(true);
        }

        String owner() {
            Thread lockOwner;
            if ((lockOwner = super.getOwner()) != null) {
                return lockOwner.getName() + " executing " + info;
            }
            return "Unknown executing " + info;
        }

        String getOwnerThreadName() {
            return Optional.ofNullable(super.getOwner()).map(Thread::getName).orElse(null);
        }

        public boolean tryLock(String info, long timeout, TimeUnit unit) throws InterruptedException {
            boolean result = super.tryLock(timeout, unit);
            if (result) {
                this.info = info;
            }
            return result;
        }

        @Override
        public void unlock() {
            super.unlock();
            info = null;
        }
    }

    // Provides exclusive access to shared state
    static protected final CustomReentrantLock lock = new CustomReentrantLock();

    protected GlobalLock() {
    }

    /**
     * @return Defaults to 10 seconds.
     */
    static public int getLockTimeoutMillis() {
        return 30000;
    }

    /**
     * Obtain the lock within {@link #getLockTimeoutMillis()} or throw {@link IllegalStateException}.
     *
     * @param info     An informal text that is printed in log messages.
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
     *
     * @param info     An informal text that is printed in log messages.
     * @param supplier The guarded code to execute while holding the lock.
     */
    static public <R> R withLockReturning(String info, Supplier<R> supplier) {
        try {
            long startTime = System.currentTimeMillis();
            if (lock.tryLock(info, getLockTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                LOG.finest("+ Acquired lock (count: " + lock.getHoldCount() + "): " + info);
                String threadName = lock.getOwnerThreadName();
                try {
                    return supplier.get();
                } finally {
                    LOG.finest("- Releasing lock (count: " + lock.getHoldCount() + "): " + info);
                    if (!lock.isHeldByCurrentThread()) {
                        LOG.severe("Lock is held by another thread, ensure the same thread acquires and releases the lock!");
                    } else {
                        lock.unlock();
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    if (duration > LOCK_WARNING_MILLIS) {
                        LOG.warning("Lock duration was above warning threshold for thread (" + threadName + "): " + duration + "ms");
                    } else {
                        LOG.fine("Lock duration for thread (" + threadName + "): " + duration + "ms");
                    }
                }
            } else {
                throw new IllegalStateException(
                    "Could not acquire lock owned by " + lock.owner() + " after waiting " + getLockTimeoutMillis() + "ms: " + Thread.currentThread().getName() + " executing " + info
                );
            }
        } catch (InterruptedException ex) {
            LOG.log(INFO, "Interrupted while waiting for lock: " + info);
            return null;
        }
    }
}
