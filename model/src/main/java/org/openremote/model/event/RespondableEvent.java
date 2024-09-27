package org.openremote.model.event;

import java.util.function.Consumer;

/**
 * For events that require a response, the consumers of these messages are responsible for calling the response consumer
 */
public interface RespondableEvent {
    Consumer<Event> getResponseConsumer();

    void setResponseConsumer(Consumer<Event> responseConsumer);
}
