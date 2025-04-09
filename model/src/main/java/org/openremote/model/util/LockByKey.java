/*
 * Copied from: https://github.com/eugenp/tutorials/blob/master/core-java-modules/core-java-concurrency-advanced-4/src/main/java/com/baeldung/lockbykey/LockByKey.java
 * Altered to use a Semaphore instead of ReentrantLock to avoid issues with camel thread context switching between lock/unlock calls (see issue #1812)
 */

package org.openremote.model.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class LockByKey {

    private static final System.Logger LOG = System.getLogger(LockByKey.class.getName());

    private static class LockWrapper {
        private final Semaphore lock = new Semaphore(1); // Don't change to ReentrantLock as camel switches thread between try/finally blocks on route processors
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

    public void lock(String key) {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        try {
            lockWrapper.lock.acquire();
            LOG.log(System.Logger.Level.TRACE, () -> "Lock acquired: key=" + key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException("Interrupted while acquiring lock for key: " + key, e);
        }
    }

    public void unlock(String key) {
        LockWrapper lockWrapper = locks.get(key);
        if (lockWrapper != null) {
            LOG.log(System.Logger.Level.TRACE, () -> "Lock release: key=" + key);
            lockWrapper.lock.release();
            if (lockWrapper.removeThreadFromQueue() == 0) {
                // NB : We pass in the specific value to remove to handle the case where another thread would queue right before the removal
                locks.remove(key, lockWrapper);
            }
        }
    }

}
