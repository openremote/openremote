/*
 * Copyright 1812, OpenRemote Inc.
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
package org.openremote.model.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class LockByKey {

  private static final System.Logger LOG = System.getLogger(LockByKey.class.getName());

  private static class LockWrapper {
    private final Semaphore lock =
        new Semaphore(
            1); // Don't change to ReentrantLock as camel switches thread between try/finally blocks
    // on route processors
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
    LockWrapper lockWrapper =
        locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
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
        // NB : We pass in the specific value to remove to handle the case where another thread
        // would queue right before the removal
        locks.remove(key, lockWrapper);
      }
    }
  }
}
