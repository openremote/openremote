package org.openremote.container;

public interface ContainerService {

    void prepare(Container container);

    void start(Container container);

    void stop(Container container);

}
