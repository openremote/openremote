package org.openremote.agent.protocol.tradfri.device.event;

import java.lang.reflect.ParameterizedType;

/**
 * The class that handles events for IKEA TRÅDFRI devices
 * @author Stijn Groenen
 * @version 1.0.0
 */
public abstract class EventHandler<T extends Event> {

    /**
     * Construct the EventHandler class
     * @since 1.0.0
     */
    public EventHandler(){
    }

    /**
     * Handle the event
     * @param event The event that occurred
     * @since 1.0.0
     */
    public abstract void handle(T event);

    /**
     * Get the class of the event that this event handler handles
     * @return The class of the event that this event handler handles
     */
    public Class<T> getEventType(){
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

}
