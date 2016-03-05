package org.openremote.manager.server;

import org.openremote.container.Container;

public class Server {

    public static void main(String[] args) {

        Container container = new Container(new SampleDataService());

        container.start();
    }
}
