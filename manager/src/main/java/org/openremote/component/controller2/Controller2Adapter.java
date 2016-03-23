package org.openremote.component.controller2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * TODO This is the class you must implement. An instance of this adapter is one controller "session".
 */
public class Controller2Adapter {

    private static final Logger LOG = Logger.getLogger(Controller2Adapter.class.getName());

    /**
     * Lifecycle of a controller "session". The first endpoint in a route will open
     * the adapter. If all routes with endpoints are stopped, the adapter will be closed.
     */
    public interface Manager {
        Controller2Adapter openAdapter(String hostPortKey); // TODO: Add more options if needed
        void closeAdapter(Controller2Adapter adapter);
    }

    /**
     * This is a reference counting manager per hostPortKey.
     */
    final static public Manager DEFAULT_MANAGER = new Manager() {
        final protected List<Controller2Adapter> adapters = new ArrayList<>();

        @Override
        synchronized public Controller2Adapter openAdapter(String hostPortKey) {
            // If adapter exists, increment reference count and return
            for (Controller2Adapter adapter : adapters) {
                if (adapter.getHostPortKey().equals(hostPortKey)) {
                    adapter.referenceCount++;
                    return adapter;
                }
            }
            Controller2Adapter adapter = new Controller2Adapter(hostPortKey);
            adapters.add(adapter);
            return adapter;
        }

        @Override
        synchronized public void closeAdapter(Controller2Adapter adapter) {
            Iterator<Controller2Adapter> it = adapters.iterator();
            while (it.hasNext()) {
                Controller2Adapter next = it.next();
                if (next.getHostPortKey().equals(adapter.getHostPortKey())) {
                    // Count references down, if zero, close and remove
                    adapter.referenceCount--;
                    if (adapter.referenceCount == 0) {
                        adapter.close();
                        it.remove();
                    }
                    break;
                }
            }
        }
    };

    public interface DiscoveryListener {
        void onDiscovery(List<String> list); // TODO Use proper device type instead of strings?
    }

    public interface SensorListener {
        void onUpdate(String state); // TODO use proper type instead of strings?
    }

    protected volatile int referenceCount = 1; // Only remove adapter if no endpoint references it
    final String hostPortKey;
    final protected List<DiscoveryListener> discoveryListeners = new CopyOnWriteArrayList<>();
    final protected List<SensorListener> sensorListeners = new CopyOnWriteArrayList<>();

    public Controller2Adapter(String hostPortKey) {
        LOG.info("### CREATING ADAPTER: " + hostPortKey);
        this.hostPortKey = hostPortKey;
    }

    public String getHostPortKey() {
        return hostPortKey;
    }

    public void close() {
        LOG.info("### CLOSING ADAPTER: " + hostPortKey);
        // TODO: Implement cleanup if needed
    }

    public void addDiscoveryListener(DiscoveryListener listener) {
        LOG.info("### ADDING DISCOVERY LISTENER: " + listener);
        discoveryListeners.add(listener);
    }

    public void removeDiscoveryListener(DiscoveryListener listener) {
        LOG.info("### REMOVING DISCOVERY LISTENER: " + listener);
        discoveryListeners.remove(listener);
    }

    public void triggerDiscovery() {
        LOG.info("### TRIGGERING DISCOVERY");
        // TODO: This should, at some point (asynchronous?) call the registered discovery listeners
    }

    public void addSensorListener(SensorListener listener) {
        LOG.info("### ADDING SENSOR LISTENER: " + listener);
        sensorListeners.add(listener);
    }

    public void removeSensorListener(SensorListener listener) {
        LOG.info("### REMOVING SENSOR LISTENER: " + listener);
        sensorListeners.remove(listener);
    }

    /**
     * TODO: If a producer endpoint wants to send a command, it calls this
     */
    public void sendCommand(String command, String argument) {
        LOG.info("### SENDING COMMAND: " + command + " - " + argument);
    }

}
