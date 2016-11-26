package org.openremote.controller.statuscache;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventContext;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.model.Sensor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatusCache {

    private static final Logger LOG = Logger.getLogger(StatusCache.class.getName());

    /**
     * Maintains a map of sensor ids to their values in cache.
     */
    final protected SensorMap sensorMap;

    /**
     * TODO : Map of sensor IDs to actual sensor instances.
     */
    final protected Map<Integer, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * A chain of event processors that incoming events (values) are forced through before
     * their values are stored in the cache.
     *
     * Event processors may modify the existing values, discard events entirely or spawn
     * multiple other events that are included in the state cache.
     */
    final protected EventProcessorChain eventProcessorChain;

    /**
     * Used to indicate if the state cache is in the middle of a shut down process -- this
     * flag can be used by methods to fail-fast in such cases.
     */
    private volatile Boolean isShutdownInProcess = false;

    public StatusCache(ChangedStatusTable cst, EventProcessorChain epc) {
        this.eventProcessorChain = epc;
        this.sensorMap = new SensorMap(cst);
    }

    public void start() {
        eventProcessorChain.start();
    }

    /**
     * Register a sensor with this cache instance. The registered sensor will participate in
     * cache's lifecycle.
     *
     * @param sensor sensor to register
     */
    public synchronized void registerSensor(Sensor sensor) {
        // TODO :
        //   push thread synchronization down to sensorMap.init() once
        //   Sensor references are handled there
        //                                                    [JPL]

        if (isShutdownInProcess) {
            return;
        }

        Sensor previous = sensors.put(sensor.getSensorID(), sensor);

        // Use a specific log category just to log the creation of sensor objects
        // in this method (happens at startup or soft restart)...

        if (previous != null) {
            throw new IllegalArgumentException(
                "Duplicate registration of sensor ID '" + sensor.getSensorID() + "', already registered as sensor named '" + previous.getName()
            );
        }

        sensorMap.init(sensor);
        LOG.info("Registered sensor: " + sensor);
    }

    /**
     * Returns a sensor instance associated with the given ID.
     *
     * @param id sensor ID
     * @return sensor instance
     */
    public Sensor getSensor(int id) {
        return sensors.get(id);
    }

    /**
     * Performs a state cache cleanup at shut down. This method is synchronized, preventing
     * concurrent thread access to shutdown steps.
     *
     * Part of the shutdown of state cache:
     * <ul>
     * <li>registered sensors are stopped</li>
     * <li>the in-memory states are cleared</li>
     * <li>registered sensors are unregistered</li>
     * </ul>
     *
     * This allows more orderly cleanup of the resources associated with this state cache --
     * namely the sensor threads are allowed to cleanup and stop properly.
     *
     * Once the shutdown is completed, this cache instance can be discarded. There's no corresponding
     * start operation to allow reuse of this object.
     */
    public synchronized void shutdown() {
        try {
            isShutdownInProcess = true;
            eventProcessorChain.stop();
            stopSensors();
            sensorMap.clear();
            sensors.clear();
        } finally {
            isShutdownInProcess = false;
        }
    }

    /**
     * Updates an incoming event value into cache.
     * This method is currently synchronized to restrict concurrency -- events are processed
     * and updated one-by-one. The implications of concurrent event processing through the processors
     * and concurrent updates must be evaluated. See ORCJAVA-205.
     *
     * @param event the event to process -- the actual value stored in this cache will depend
     *              on the modifications made by event processors associated with this cache
     */
    public synchronized void update(Event event) {
        LOG.fine("==> Updating status for event: " + event);
        if (isShutdownInProcess) {
            LOG.fine("<== Status cache is shutting down. Ignoring update from: " + event.getSource());
            return;
        }

        // push incoming event through processing chain -- keep the last returned instance including
        // modifications if any...

        EventContext ctx = new EventContext(this, event);

        eventProcessorChain.push(ctx);

        // Update the final value...

        if (!ctx.hasTerminated()) {
            sensorMap.update(ctx.getEvent());
            LOG.fine("<== Updating status complete for event: " + event);
        } else {
            LOG.fine("<== Updating status complete, event context terminated, no update was made to status cache for event: " + ctx.getEvent());
        }
    }

    public Map<Integer, String> queryStatus(Set<Integer> sensorIDs) {
        if (sensorIDs == null || sensorIDs.size() == 0) {
            return null;     // TODO : return an empty collection instead
        }
        LOG.fine("Query status for sensor IDs: " + sensorIDs);
        Map<Integer, String> statuses = new HashMap<Integer, String>();
        for (Integer sensorId : sensorIDs) {
            statuses.put(sensorId, queryStatus(sensorId));
        }
        LOG.fine("Returning sensor status map (ID, Value): " + statuses);
        return statuses;
    }

    /**
     * Returns the current in-memory state of the given sensor ID.
     *
     * @param sensorID requested sensor ID
     * @return current cache-stored value for the given sensor ID
     */
    public String queryStatus(Integer sensorID) {
        if (!sensorMap.hasExistingState(sensorID)) {
            LOG.info("Requested sensor id '" + sensorID + "' was not found. Defaulting to: " + Sensor.UNKNOWN_STATUS);
            return Sensor.UNKNOWN_STATUS;
        }
        return sensorMap.getCurrentState(sensorID).serialize();
    }

    public Event queryStatus(String name) throws Exception {
        return sensorMap.get(name);
    }

    public Iterator<Event> getStateSnapshot() {
        return sensorMap.getSnapshot();
    }

    public Integer sensorIDFromName(String sensorName) {
        return sensorMap.getSensorID(sensorName);
    }

    private void stopSensors() {
        for (Sensor sensor : sensors.values()) {
            LOG.info("Stopping sensor: " + sensor);

            try {
                sensor.stop();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to stop sensor: " + sensor, t);
            }
        }
    }

    private class SensorMap {

        private Map<String, Integer> nameIdIndex = new ConcurrentHashMap<>();
        private Map<Integer, Event> currentState = new ConcurrentHashMap<>();
        private ChangedStatusTable deviceStatusChanges;

        private SensorMap(ChangedStatusTable cst) {
            this.deviceStatusChanges = cst;
        }

        private void init(Sensor sensor) {
            nameIdIndex.put(sensor.getName(), sensor.getSensorID());
            currentState.put(sensor.getSensorID(), new Sensor.UnknownEvent(sensor));
        }

        private Iterator<Event> getSnapshot() {
            return currentState.values().iterator();
        }

        private void clear() {
            clearDeviceStatusChanges();

            nameIdIndex.clear();
            currentState.clear();
        }

        private void clearDeviceStatusChanges() {
            for (Sensor sensor : sensors.values()) {
                // Just wake up all the records, acturelly, the status didn't change.
                deviceStatusChanges.updateStatusChangedIDs(sensor.getSensorID());
            }
            deviceStatusChanges.clearAllRecords();
        }


        private boolean hasExistingState(int id) {
            return currentState.get(id) != null;
        }

        private Event getCurrentState(int id) {
            return currentState.get(id);
        }

        private Event get(String name) throws Exception {
            if (!nameIdIndex.keySet().contains(name)) {
                throw new Exception("No sensors with name: " + name);
            }
            int id = nameIdIndex.get(name);
            return currentState.get(id);
        }

        private Integer getSensorID(String sensorName) {
            return nameIdIndex.get(sensorName);
        }

        private void update(Event event) {
            int id = event.getSourceID();

            if (currentState.get(id) == null) {
                LOG.fine("Starting value cache for sensor: " +  event.getSource());
                currentState.put(id, event);
            } else {
                Event previousState = currentState.get(id);
                if (previousState.isEqual(event)) {
                    return;
                }
                LOG.fine("Updating value cache for sensor: " +  event.getSource());
                currentState.put(id, event);
            }

            deviceStatusChanges.updateStatusChangedIDs(event.getSourceID());
            LOG.fine("Marking changed sensor: " + event.getSource());
        }
    }
}
