/*
 * Copied from: https://github.com/eugenp/tutorials/blob/master/core-java-modules/core-java-concurrency-advanced-4/src/main/java/com/baeldung/lockbykey/LockByKey.java
 * Altered to use a Semaphore instead of ReentrantLock to avoid issues with camel thread context switching between lock/unlock calls (see issue #1812)
 */

package org.openremote.model.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockByKey {

    private static final System.Logger LOG = System.getLogger(LockByKey.class.getName());
    private static final long DEFAULT_TIMEOUT_MILLIS = 10000;

    private static class LockWrapper {
        private final Lock lock = new ReentrantLock(true);
        private final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);

        private LockWrapper addThreadInQueue() {
            numberOfThreadsInQueue.incrementAndGet();
            return this;
        }

        private int removeThreadFromQueue() {
            return numberOfThreadsInQueue.decrementAndGet();
        }

    }

    private final Map<String, LockWrapper> locks = new ConcurrentHashMap<>();

    public void lock(String key) throws TimeoutException {
        lock(key, DEFAULT_TIMEOUT_MILLIS);
    }

    public void lock(String key, long timeoutMillis) throws TimeoutException {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        try {
            boolean success = lockWrapper.lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!success) {
                if (lockWrapper.removeThreadFromQueue() == 1) {
                    locks.remove(key, lockWrapper);
                }
                throw new TimeoutException("Timeout reached whilst waiting to acquire lock");
            }
            LOG.log(System.Logger.Level.TRACE, () -> "Lock acquired: key=" + key + ", threadName=" + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            if (lockWrapper.removeThreadFromQueue() == 1) {
                locks.remove(key, lockWrapper);
            }
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException("Interrupted while acquiring lock for key: " + key, e);
        }
    }

    public boolean tryLock(String key) {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        if (lockWrapper.lock.tryLock()) {
            LOG.log(System.Logger.Level.TRACE, () -> "Lock acquired: key=" + key + ", threadName=" + Thread.currentThread().getName());
            return true;
        } else {
            if (lockWrapper.removeThreadFromQueue() == 0) {
                locks.remove(key, lockWrapper);
            }
            return false;
        }
    }

    public void unlock(String key) {
        locks.compute(key, (k, lockWrapper) -> {
            if (lockWrapper != null) {
                LOG.log(System.Logger.Level.TRACE, () -> "Lock release: key=" + key + ", threadName=" + Thread.currentThread().getName());
                lockWrapper.lock.unlock();
                if (lockWrapper.removeThreadFromQueue() == 0) {
                    return null; // Remove the entry
                }
            }
            return lockWrapper;
        });
    }

}
