/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.timer;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getString;

/**
 * Wall real clock timer or pseudo clock time (for testing).
 */
public class TimerService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(TimerService.class.getName());
    public static final String TIMER_CLOCK_TYPE = "TIMER_CLOCK_TYPE";
    public static final String TIMER_CLOCK_TYPE_DEFAULT = Clock.REAL.toString();
    public static final int PRIORITY = ContainerService.HIGH_PRIORITY + 300;

    public enum Clock {
        REAL {
            @Override
            public void init() {
                // NOOP
            }

            @Override
            public long getCurrentTimeMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public long advanceTime(long amount, TimeUnit unit) {
                throw new UnsupportedOperationException("Wall clock can not be advanced manually");
            }
        },
        PSEUDO {
            AtomicLong timer = new AtomicLong(System.currentTimeMillis());

            @Override
            public void init() {
                timer.set(System.currentTimeMillis());
                long current = timer.get();
                LOG.info("Initialized pseudo clock to: " + (current) + "/" + new Date(current));
            }

            @Override
            public long getCurrentTimeMillis() {
                return timer.get();
            }

            @Override
            public long advanceTime(long amount, TimeUnit unit) {
                return timer.addAndGet(unit.toMillis(amount));
            }
        };

        public abstract void init();
        public abstract long getCurrentTimeMillis();
        public abstract long advanceTime(long amount, TimeUnit unit);
    }

    protected Clock clock;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.clock = Clock.valueOf(
            getString(container.getConfig(), TIMER_CLOCK_TYPE, TIMER_CLOCK_TYPE_DEFAULT)
        );
        this.clock.init();
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Clock getClock() {
        return clock;
    }

    public long getCurrentTimeMillis() {
        return getClock().getCurrentTimeMillis();
    }

    public Instant getNow() {
        return Instant.ofEpochMilli(getCurrentTimeMillis());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "clock=" + clock +
            '}';
    }
}
