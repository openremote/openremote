package org.openremote.controller.context;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventProcessingContext;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.model.Deployment;
import org.openremote.controller.model.Sensor;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataContext {

    private static final Logger LOG = Logger.getLogger(DataContext.class.getName());

    final protected Deployment deployment;
    final protected EventProcessorChain eventProcessorChain;

    final protected Map<Integer, Sensor> sensors = new ConcurrentHashMap<>();
    final protected Map<Integer, Event> sensorState = new ConcurrentHashMap<>();

    /**
     * Used to indicate if the data context is in the middle of a shut down process -- this
     * flag can be used by methods to fail-fast in such cases.
     */
    private volatile Boolean isShutdownInProcess = false;

    public DataContext(Deployment deployment, EventProcessorChain eventProcessorChain) {
        this.deployment = deployment;
        this.eventProcessorChain = eventProcessorChain;
    }

    public synchronized void start() {
        if (isShutdownInProcess)
            return;
        eventProcessorChain.start();
    }

    /**
     * <ul>
     * <li>registered sensors are stopped</li>
     * <li>the current state is cleared</li>
     * <li>registered sensors are unregistered</li>
     * </ul>
     */
    public synchronized void stop() {
        try {
            isShutdownInProcess = true;
            eventProcessorChain.stop();
            for (Sensor sensor : sensors.values()) {
                LOG.info("Stopping sensor: " + sensor);
                try {
                    sensor.stop();
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Failed to stop sensor: " + sensor, t);
                }
            }
            sensorState.clear();
            sensors.clear();
        } finally {
            isShutdownInProcess = false;
        }
    }

    public synchronized void registerAndStartSensor(Sensor sensor) {
        if (isShutdownInProcess) {
            return;
        }

        Sensor previous = sensors.put(sensor.getSensorDefinition().getSensorID(), sensor);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate registration: " + sensor.getSensorDefinition());
        }

        // Initial state
        sensorState.put(sensor.getSensorDefinition().getSensorID(), new Sensor.UnknownEvent(sensor));

        sensor.start(this);
        LOG.info("Registered and started sensor: " + sensor);
    }

    public synchronized void update(Event event) {
        LOG.fine("==> Update from event: " + event);
        if (isShutdownInProcess) {
            LOG.fine("<== Data context is shutting down. Ignoring update from: " + event.getSource());
            return;
        }

        EventProcessingContext ctx = new EventProcessingContext(this, event);
        eventProcessorChain.push(ctx);

        // Early exist if one of the processors decided to terminate the chain
        if (ctx.hasTerminated()) {
            LOG.fine("<== Updating status complete, event context terminated, no update was made to data context for event: " + ctx.getEvent());
            return;
        }

        int sourceID = event.getSourceID();
        if (sensorState.get(sourceID) == null) {
            LOG.fine("Inserted: " + event);
            sensorState.put(sourceID, event);
        } else {
            Event previousState = sensorState.get(sourceID);
            if (previousState.isEqual(event)) {
                return;
            }
            LOG.fine("Updated: " + event);
            sensorState.put(sourceID, event);
        }
        LOG.fine("<== Updating status complete for event: " + event);
        // TODO: Trigger notification of client that stuff has changed? Put it in a message broker/queue/topic?
    }

    public String queryValue(int sensorID) {
        if (!sensorState.containsKey(sensorID)) {
            LOG.info("Requested sensor id '" + sensorID + "' was not found. Defaulting to: " + Sensor.UNKNOWN_STATUS);
            return Sensor.UNKNOWN_STATUS;
        }
        return sensorState.get(sensorID).serialize();
    }

    public String queryValue(String sensorName) {
        return queryValue(deployment.getSensorID(sensorName));
    }

    public Event queryEvent(int sensorID) {
        return sensorState.get(sensorID);
    }

    public Event queryEvent(String sensorName) {
        Integer sensorID = deployment.getSensorID(sensorName);
        return sensorID != null ? queryEvent(sensorID) : null;
    }

    public Iterator<Event> getFullSnapshot() {
        return sensorState.values().iterator();
    }
}
