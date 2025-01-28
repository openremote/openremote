/*
 * Copyright 2025, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockByKey {

  private static class LockWrapper {
    private final Lock lock = new ReentrantLock();
    private final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);

    private LockWrapper addThreadInQueue() {
      numberOfThreadsInQueue.incrementAndGet();
      return this;
    }

    private int removeThreadFromQueue() {
      return numberOfThreadsInQueue.decrementAndGet();
    }
  }

  private Map<String, LockWrapper> locks = new ConcurrentHashMap<>();

  public void lock(String key) {
    LockWrapper lockWrapper =
        locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
    lockWrapper.lock.lock();
  }

  public void unlock(String key) {
    LockWrapper lockWrapper = locks.get(key);
    lockWrapper.lock.unlock();
    if (lockWrapper.removeThreadFromQueue() == 0) {
      // NB : We pass in the specific value to remove to handle the case where another thread would
      // queue right before the removal
      locks.remove(key, lockWrapper);
    }
  }
}
