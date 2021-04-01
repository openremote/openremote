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

import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

            @Override
            public void stop() {
                // NOOP
            }

            @Override
            public void start() {
                // NOOP
            }

            @Override
            public void reset() {
                // NOOP
            }
        },
        PSEUDO {
            protected AtomicLong offset = new AtomicLong();
            protected Long stopTime;

            @Override
            public void init() {
                long current = getCurrentTimeMillis();
                LOG.info("Initialized pseudo clock to: " + (current) + "/" + new Date(current));
            }

            @Override
            public long getCurrentTimeMillis() {
                return (stopTime != null ? stopTime : System.currentTimeMillis()) + offset.get();
            }

            @Override
            public long advanceTime(long amount, TimeUnit unit) {
                offset.addAndGet(unit.toMillis(amount));
                return getCurrentTimeMillis();
            }

            @Override
            public void stop() {
                if (stopTime == null) {
                    stopTime = System.currentTimeMillis();
                }
            }

            @Override
            public void start() {
                if (stopTime != null) {
                    stopTime = null;
                }
            }

            @Override
            public void reset() {
                offset.set(0L);
            }
        };

        public abstract void init();
        public abstract long getCurrentTimeMillis();
        public abstract void stop();
        public abstract void start();
        public abstract void reset();
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
        getClock().start();
    }

    @Override
    public void stop(Container container) throws Exception {
        getClock().stop();
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
