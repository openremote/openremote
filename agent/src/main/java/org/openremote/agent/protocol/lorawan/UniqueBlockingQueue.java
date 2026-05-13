/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class UniqueBlockingQueue<T> {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final Set<T> set = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public boolean put(T item) throws InterruptedException {
        lock.lock();
        try {
            if (set.add(item)) {
                queue.put(item);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        T item = queue.take();

        lock.lock();
        try {
            set.remove(item);
            return item;
        } finally {
            lock.unlock();
        }
    }

    public T peek() {
        lock.lock();
        try {
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            queue.clear();
            set.clear();
        } finally {
            lock.unlock();
        }
    }
}
