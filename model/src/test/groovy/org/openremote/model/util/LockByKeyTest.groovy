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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class LockByKeyTest extends Specification {

    def "threads are not starved when they arrive while the previous lock is removed"() {
        given:
        def lockByKey = new ControllableLockByKey()
        def key = "testKey"
        ExecutorService executor = Executors.newFixedThreadPool(3)
        def waitingThreads = new CountDownLatch(2)

        when: "the owner pauses while removing the final waiter"
        Future<?> owner = executor.submit {
            lockByKey.lock(key)
            lockByKey.unlock(key)
        }
        assert lockByKey.waitForPause()

        and: "two other threads try to acquire the same key"
        List<Future<?>> waiters = (1..2).collect {
            executor.submit {
                waitingThreads.countDown()
                lockByKey.lock(key)
                lockByKey.unlock(key)
            }
        }
        assert waitingThreads.await(1, TimeUnit.SECONDS)
        lockByKey.resume()

        then:
        owner.get(1, TimeUnit.SECONDS) == null
        waiters*.get(1, TimeUnit.SECONDS) == [null, null]
        owner.done
        waiters.every { it.done }

        cleanup:
        lockByKey.resume()
        executor.shutdownNow()
    }

    /**
     * A controllable LockByKey that pauses unlock at the point where the final queued thread is
     * removed.
     */
    static class ControllableLockByKey extends LockByKey {
        private final CountDownLatch pauseLatch = new CountDownLatch(1)
        private final CountDownLatch resumeLatch = new CountDownLatch(1)

        @Override
        protected LockWrapper createLockWrapper() {
            new LockWrapper() {
                @Override
                protected int removeThreadFromQueue() {
                    int result = super.removeThreadFromQueue()
                    if (result == 0) {
                        pauseLatch.countDown()
                        resumeLatch.await()
                    }
                    result
                }
            }
        }

        boolean waitForPause() {
            pauseLatch.await(1, TimeUnit.SECONDS)
        }

        void resume() {
            resumeLatch.countDown()
        }
    }
}
