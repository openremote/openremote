package org.openremote.container;

public interface ContainerService {

    /**
     * All services are initialized in the order they have been added to container.
     */
    void init(Container container) throws Exception;

    /**
     * After initialization, services are configured in reverse order.
     */
    void configure(Container container) throws Exception;

    /**
     * After configuration, services are started in the order they have been added to container.
     */
    void start(Container container) throws Exception;

    /**
     * When the container is shutting down, it stops all services in the order they have been added to container.
     */
    void stop(Container container) throws Exception;

}
