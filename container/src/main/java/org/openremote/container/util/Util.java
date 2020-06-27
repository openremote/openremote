package org.openremote.container.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openremote.container.Container;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {

    public static class Retry {

        protected String name;
        protected ScheduledExecutorService executorService;
        protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        protected Logger logger;
        protected long initialDelay;
        protected long maxDelay = 5*60000L;
        protected long backoffMultiplier = 2L;
        protected long jitterMargin = 10000L;
        protected int maxRetries = Integer.MAX_VALUE;
        protected boolean running;
        protected int retries;
        Supplier<Boolean> task;
        Future<?> future;
        Runnable successCallback;
        Runnable failureCallback;

        public Retry(String name, ScheduledExecutorService executorService, Supplier<Boolean> task) {
            this.name = name;
            this.executorService = executorService;
            this.task = task;
        }

        public Retry setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public Retry setLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Retry setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Retry setMaxDelay(long maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Retry setBackoffMultiplier(long backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Retry setJitterMargin(long jitterMargin) {
            this.jitterMargin = jitterMargin;
            return this;
        }

        public Retry setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Retry setSuccessCallback(Runnable successCallback) {
            this.successCallback = successCallback;
            return this;
        }

        public Retry setFailureCallback(Runnable failureCallback) {
            this.failureCallback = failureCallback;
            return this;
        }

        public synchronized void run() {
            if (running) {
                throw new IllegalStateException(name + ": already running");
            }

            retries = 1;
            running = true;

            if (initialDelay > 0) {
                future = executorService.schedule(this::doAttempt, initialDelay, timeUnit);
            } else {
                future = executorService.submit(this::doAttempt);
            }
        }

        public synchronized void cancel(boolean mayInterrupt) {
            if (!running) {
                return;
            }

            running = false;
            future.cancel(mayInterrupt);
        }

        protected void doAttempt() {
            boolean success = false;
            boolean retry = retries < maxRetries;

            try {
                log(Level.INFO, name + ": running, attempt=" + retries, null);
                success = task.get();
            } catch (Exception e) {
                log(Level.INFO, name + ": threw an exception", e);
            }

            if (!running) {
                return;
            }

            if (success) {
                if (successCallback != null) {
                    successCallback.run();
                }
                return;
            }

            if (!retry) {
                if (failureCallback != null) {
                    failureCallback.run();
                }
                return;
            }

            long adjustedMax = Math.max(maxDelay - jitterMargin, jitterMargin);
            long delay = Math.max(initialDelay, Math.min(initialDelay * (long)Math.pow(2, retries), adjustedMax));
            delay += Math.random()*jitterMargin;
            retries++;

            log(Level.INFO, name + ": scheduling retry in " + delay + " " + timeUnit.name(), null);
            future = executorService.schedule(this::doAttempt, delay, timeUnit);
        }

        protected void log(Level level, String log, Throwable e) {
            if (logger != null) {
                logger.log(level, log, e);
            }
        }
    }

    /**
     * Convert a JSON serialisable object to an {@link ObjectValue}
     */
    public static Optional<ObjectValue> objectToValue(@NotNull Object obj) {
        try {
            String json = Container.JSON.writeValueAsString(obj);
            return Values.parse(json);
        } catch (JsonProcessingException ignore) {}
        return Optional.empty();
    }

    public static <T> T[] reverseArray(T[] array, Class<T> clazz) {
        if (array == null) {
            return null;
        }
        T[] newArray = createArray(array.length, clazz);
        int j = 0;
        for (int i=array.length; i>0; i--) {
            newArray[j] = array[i-1];
            j++;
        }
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(int size, Class<T> clazz) {
        return (T[]) Array.newInstance(clazz, size);
    }

    /**
     * @param o A timestamp string as 'HH:mm:ss' or 'HH:mm'.
     * @return Epoch time or 0 if there is a problem parsing the timestamp string.
     */
    public static long parseTimestamp(Object o) {
        String timestamp = "";
        try {
            timestamp = o.toString();
        }catch (Exception e){
            return (0L);
        }
        SimpleDateFormat sdf;
        if (timestamp.length() == 8) {
            sdf = new SimpleDateFormat("HH:mm:ss");
        } else if (timestamp.length() == 5) {
            sdf = new SimpleDateFormat("HH:mm");
        } else {
            return (0L);
        }
        try {
            return (sdf.parse(timestamp).getTime());
        } catch (ParseException e) {
            return (0L);
        }
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'HH:mm:ss' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatTimestampWithSeconds(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param timestamp Epoch time
     * @return The timestamp formatted as 'EEE' or <code>null</code> if the timestamp is <= 0.
     */
    public static String formatDayOfWeek(long timestamp) {
        if (timestamp <= 0)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE");
        return (sdf.format(new Date(timestamp)));
    }

    /**
     * @param o       A timestamp string as 'HH:mm' or '-'.
     * @param minutes The minutes to increment/decrement from timestamp.
     * @return Timestamp string as 'HH:mm', modified with the given minutes or the current time + 60 minutes if
     * the given timestamp was '-' or the given timestamp couldn't be parsed.
     */
    public static String shiftTime(Object o, int minutes) {
        String timestamp = o.toString();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = null;
        if (timestamp != null && timestamp.length() >= 1 && timestamp.substring(0, 1).equals("-")) {
            date = new Date();
            date.setTime(date.getTime() + 60 * 60000);
        } else {
            try {
                date = sdf.parse(timestamp);
                date.setTime(date.getTime() + minutes * 60000);
            } catch (ParseException ex) {
                date = new Date();
                date.setTime(date.getTime() + 60 * 60000);
            }
        }
        return (sdf.format(date));
    }

    /**
     * @param o A string representation of a double value.
     * @return The parsed value or 0.0 if the string couldn't be parsed.
     */
    public static Double parseDouble(Object o) {
        String s = o.toString();
        try {
            if (s.length() >= 1) {
                return (Double.parseDouble(s.substring(0, s.length() - 1)));
            } else {
                return (0.0);
            }
        } catch (NumberFormatException e) {
            return (0.0);
        }
    }

    /**
     * @param o A string representation of a double value.
     * @param shift Increments or decrements the parsed value.
     * @param suffix A string appended to the result.
     */
    public static String shiftDouble(Object o, double shift, String suffix) {
        Double d = parseDouble(o);
        d = d + shift;
        return (String.format("%.1f", d) + suffix);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @SafeVarargs
    public static <T> List<T> joinCollections(Collection<T>...collections) {
        if (collections == null || collections.length == 0) {
            return Collections.emptyList();
        }

        List<T> newCollection = null;

        for (Collection<T> collection : collections) {
            if (collection == null) {
                continue;
            }

            if (newCollection == null) {
                newCollection = new ArrayList<>(collection);
            } else {
                newCollection.addAll(collection);
            }
        }
        return newCollection;
    }
}
