/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.util

import spock.lang.Specification

import java.util.concurrent.*

class LockByKeyTest extends Specification {
    /**
     * A controllable version of LockByKey that allows us to pause the unlock method at a critical
     * point.
     */
    static class ControllableLockByKey extends LockByKey {

        private final CountDownLatch pauseLatch = new CountDownLatch(1)
        private final CountDownLatch resumeLatch = new CountDownLatch(1)

        @Override
        protected LockWrapper createLockWrapper() {
            new LockWrapper() {
                @Override
                int removeThreadFromQueue() {
                    int result = super.removeThreadFromQueue()
                    if (result == 0) {
                        // Signal that we are at the critical point
                        pauseLatch.countDown()
                        // Wait for the test to tell us to continue
                        try {
                            resumeLatch.await()
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e)
                        }
                    }
                    result
                }
            }
        }

        void waitForPause() throws InterruptedException {
            pauseLatch.await(5, TimeUnit.SECONDS)
        }

        void resume() {
            resumeLatch.countDown()
        }

        int getQueueSize(String key) {
            LockWrapper lockWrapper = locks.get(key)
            if (lockWrapper != null) {
                return lockWrapper.numberOfThreadsInQueue.get()
            }
            0
        }
    }

    def testThreadStarvation() throws InterruptedException {
        expect:
        final ControllableLockByKey lockByKey = new ControllableLockByKey()
        final String key = "testKey"
        final ExecutorService executor = Executors.newFixedThreadPool(3)
        final CountDownLatch threadWaitingLatch = new CountDownLatch(2)
        final CountDownLatch threadResumeLatch = new CountDownLatch(1)

        // Thread 1: Acquires and releases the lock, pausing in the middle of unlock
        Future<?> future1 =
            executor.submit {
                lockByKey.lock(key)
                lockByKey.unlock(key)
            }

        // Wait for Thread 1 to pause in the unlock method
        lockByKey.waitForPause()

        // Thread 2: Tries to acquire the lock while Thread 1 is trying to unlock
        Future<?> future2 =
            executor.submit {
                try {
                    threadWaitingLatch.countDown()
                    lockByKey.lock(key)
                    threadResumeLatch.await(5, TimeUnit.SECONDS)
                } catch (InterruptedException e) {
                    throw new RuntimeException(e)
                }
                lockByKey.unlock(key)
            }

        // Thread 3: Tries to acquire the lock while Thread 1 is trying to unlock
        Future<?> future3 =
            executor.submit {
                try {
                    Thread.sleep(1000)
                    threadWaitingLatch.countDown()
                    lockByKey.lock(key)
                    threadResumeLatch.await(5, TimeUnit.SECONDS)
                } catch (InterruptedException e) {
                    throw new RuntimeException(e)
                }
                lockByKey.unlock(key)
            }

        // Wait for Thread 2 & 3 to try and acquire the lock
        threadWaitingLatch.await(5, TimeUnit.SECONDS)

        def counter = 0
        while (lockByKey.getQueueSize(key) != 2 && counter < 10) {
            Thread.sleep(100)
            counter++
        }

        // Allow Thread 1 to complete its unlock and remove the LockWrapper
        lockByKey.resume()

        // Wait for Thread 1 to finish
        try {
            future1.get(1, TimeUnit.SECONDS)
        } catch (Exception e) {
            throw new AssertionError("Thread 1 failed to complete", e)
        }
        assert future1.isDone(): "Thread 1 should have completed"

        // Allow Thread 2 & 3 to continue
        threadResumeLatch.countDown()

        // Wait for Thread 2 & 3 to finish
        try {
            future2.get(1, TimeUnit.SECONDS)
            future3.get(1, TimeUnit.SECONDS)
        } catch (TimeoutException e) {
            // We expect a timeout here, which indicates a thread is stuck
        } catch (Exception e) {
            throw new AssertionError("An unexpected exception occurred", e)
        }

        assert future2.isDone(): "Thread 2 should have completed"

        // Thread 3 should complete
        assert future3.isDone(): "Thread 3 should have completed"

        def ignored = executor.shutdownNow()
        true
    }
}
