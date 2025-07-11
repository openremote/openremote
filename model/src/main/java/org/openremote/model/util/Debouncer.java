/*
 * Copyright 2022, OpenRemote Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** A simple key based debounce utility */
public class Debouncer<T> {
  protected final ScheduledExecutorService scheduledExecutorService;
  protected final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>();
  protected final Consumer<T> callback;
  protected final int intervalMillis;

  public Debouncer(
      ScheduledExecutorService scheduledExecutorService, Consumer<T> c, int intervalMillis) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.callback = c;
    this.intervalMillis = intervalMillis;
  }

  public void call(T key) {
    TimerTask task = new TimerTask(key);

    TimerTask prev;
    do {
      prev = delayedMap.putIfAbsent(key, task);
      if (prev == null)
        scheduledExecutorService.schedule(task, intervalMillis, TimeUnit.MILLISECONDS);
    } while (prev != null
        && !prev.extend()); // Exit only if new task was added to map, or existing task was extended
    // successfully
  }

  public void cancelAll(boolean mayInterruptIfRunning) {
    List<TimerTask> tasks = new ArrayList<>(delayedMap.values());
    delayedMap.clear();
    tasks.forEach(task -> task.cancel(mayInterruptIfRunning));
  }

  // The task that wakes up when the wait time elapses
  protected class TimerTask implements Runnable {
    private final T key;
    private long dueTime;
    private final Object lock = new Object();
    private ScheduledFuture<?> scheduledFuture;

    public TimerTask(T key) {
      this.key = key;
      extend();
    }

    public boolean extend() {
      synchronized (lock) {
        if (dueTime < 0) // Task has been shutdown
        return false;
        dueTime = System.currentTimeMillis() + intervalMillis;
        return true;
      }
    }

    public void run() {
      synchronized (lock) {
        long remaining = dueTime - System.currentTimeMillis();
        if (remaining > 0) { // Re-schedule task
          scheduledExecutorService.schedule(this, remaining, TimeUnit.MILLISECONDS);
        } else { // Mark as terminated and invoke callback
          dueTime = -1;
          try {
            callback.accept(key);
          } finally {
            delayedMap.remove(key);
          }
        }
      }
    }

    public void cancel(boolean mayInterruptIfRunning) {
      if (scheduledFuture != null) {
        scheduledFuture.cancel(mayInterruptIfRunning);
      }
    }
  }
}
