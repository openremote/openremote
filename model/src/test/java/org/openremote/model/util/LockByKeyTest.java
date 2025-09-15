package org.openremote.model.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class LockByKeyTest {
    /**
     * A controllable version of LockByKey that allows us to pause the unlock method at a critical point.
     */
    static class ControllableLockByKey extends LockByKey {

        private final CountDownLatch pauseLatch = new CountDownLatch(1);
        private final CountDownLatch resumeLatch = new CountDownLatch(1);

        @Override
        protected LockWrapper createLockWrapper() {
            return new LockWrapper() {
                @Override
                public int removeThreadFromQueue() {
                    int result = super.removeThreadFromQueue();
                    if (result == 0) {
                        // Signal that we are at the critical point
                        pauseLatch.countDown();
                        // Wait for the test to tell us to continue
                        try {
                            resumeLatch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return result;
                }
            };
        }

        public void waitForPause() throws InterruptedException {
            pauseLatch.await(5, TimeUnit.SECONDS);
        }

        public void resume() {
            resumeLatch.countDown();
        }

        public int getQueueSize(String key) {
            LockWrapper lockWrapper = locks.get(key);
            if (lockWrapper != null) {
                return lockWrapper.numberOfThreadsInQueue.get();
            }
            return 0;
        }
    }

    @Test
    void testThreadStarvation() throws InterruptedException {
        final ControllableLockByKey lockByKey = new ControllableLockByKey();
        final String key = "testKey";
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final CountDownLatch threadWaitingLatch = new CountDownLatch(2);
        final CountDownLatch threadResumeLatch = new CountDownLatch(1);

        // Thread 1: Acquires and releases the lock, pausing in the middle of unlock
        Future<?> future1 = executor.submit(() -> {
            lockByKey.lock(key);
            lockByKey.unlock(key);
        });

        // Wait for Thread 1 to pause in the unlock method
        lockByKey.waitForPause();

        // Thread 2: Tries to acquire the lock while Thread 1 is trying to unlock
        Future<?> future2 = executor.submit(() -> {
            try {
                threadWaitingLatch.countDown();
                lockByKey.lock(key);
                threadResumeLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lockByKey.unlock(key);
        });

        // Thread 3: Tries to acquire the lock while Thread 1 is trying to unlock
        Future<?> future3 = executor.submit(() -> {
            try {
                Thread.sleep(1000);
                threadWaitingLatch.countDown();
                lockByKey.lock(key);
                threadResumeLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lockByKey.unlock(key);
        });

        // Wait for Thread 2 & 3 to try and acquire the lock
        threadWaitingLatch.await(5, TimeUnit.SECONDS);

        var counter = 0;
        while (lockByKey.getQueueSize(key) != 2 && counter < 10) {
            Thread.sleep(100);
            counter++;
        }

        // Allow Thread 1 to complete its unlock and remove the LockWrapper
        lockByKey.resume();

        // Wait for Thread 1 to finish
        try {
            future1.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Thread 1 failed to complete", e);
        }
        assertTrue(future1.isDone(), "Thread 1 should have completed");

        // Allow Thread 2 & 3 to continue
        threadResumeLatch.countDown();

        // Wait for Thread 2 & 3 to finish
        try {
            future2.get(1, TimeUnit.SECONDS);
            future3.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // We expect a timeout here, which indicates a thread is stuck
        } catch (Exception e) {
            fail("An unexpected exception occurred", e);
        }

        assertTrue(future2.isDone(), "Thread 2 should have completed");

        // Thread 3 should complete
        assertTrue(future3.isDone(), "Thread 3 should have completed");

        executor.shutdownNow();
    }
}
