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
import spock.util.concurrent.PollingConditions

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
    }

    def "threads queued behind an unlock are not starved"() {
        given:
        def conditions = new PollingConditions(timeout: 5)
        def lockByKey = new ControllableLockByKey()
        def key = "testKey"
        def executor = Executors.newFixedThreadPool(3)
        def threadResumeLatch = new CountDownLatch(1)

        when: "a thread acquires and releases the lock, pausing in the middle of unlock"
        def future1 = executor.submit {
            lockByKey.lock(key)
            lockByKey.unlock(key)
        }
        lockByKey.waitForPause()

        and: "two more threads try to acquire the lock while the first is unlocking"
        def contenders = new CopyOnWriteArrayList<Thread>()
        def contend = {
            contenders.add(Thread.currentThread())
            lockByKey.lock(key)
            threadResumeLatch.await(5, TimeUnit.SECONDS)
            lockByKey.unlock(key)
        }
        def future2 = executor.submit(contend)
        def future3 = executor.submit(contend)

        then: "both contend for the lock the first thread is about to remove"
        conditions.eventually {
            assert contenders.size() == 2
            assert contenders.every {
                it.state == Thread.State.BLOCKED || it.state == Thread.State.WAITING
            }
        }

        when: "the first thread completes its unlock and removes the LockWrapper"
        lockByKey.resume()

        then:
        future1.get(5, TimeUnit.SECONDS) == null

        when: "the queued threads are released"
        threadResumeLatch.countDown()

        then: "neither is stuck"
        future2.get(5, TimeUnit.SECONDS) == null
        future3.get(5, TimeUnit.SECONDS) == null

        cleanup:
        executor.shutdownNow()
    }
}
