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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UniqueBlockingQueue<T> {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final Set<T> set = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean put(T item) throws InterruptedException {
        if (set.add(item)) {
            queue.put(item);
            return true;
        }
        return false;
    }

    public boolean offer(T item) {
        if (set.add(item)) {
            return queue.offer(item);
        }
        return false;
    }

    public T take() throws InterruptedException {
        T item = queue.take();
        set.remove(item);
        return item;
    }

    public T poll() {
        T item = queue.poll();
        if (item != null) {
            set.remove(item);
        }
        return item;
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        T item = queue.poll(timeout, unit);
        if (item != null) {
            set.remove(item);
        }
        return item;
    }

    public T peek() {
        return queue.peek(); // no removal, no set modification
    }

    public boolean contains(T item) {
        return set.contains(item);
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
        set.clear();
    }
}
