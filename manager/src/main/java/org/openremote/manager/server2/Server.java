package org.openremote.manager.server2;

import org.openremote.container.Container;

public class Server {

    public static void main(String[] args) {
        Container container = new Container();
        container.start();
    }
}
