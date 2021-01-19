package org.openremote.agent.protocol.tradfri.device.event;

import java.lang.reflect.ParameterizedType;

/**
 * The class that handles events for IKEA TRÅDFRI devices
 */
public abstract class EventHandler<T> {

    /**
     * Construct the EventHandler class
     */
    public EventHandler(){
    }

    /**
     * Handle the event
     * @param event The event that occurred
     */
    public abstract void handle(T event);

    /**
     * Get the class of the event that this event handler handles
     * @return The class of the event that this event handler handles
     */
    @SuppressWarnings("unchecked")
    public Class<T> getEventType(){
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

}
