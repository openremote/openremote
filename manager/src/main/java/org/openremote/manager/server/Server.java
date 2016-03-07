package org.openremote.manager.server;

import org.openremote.container.Container;

import java.util.logging.Logger;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) throws Exception {
        new Container(new SampleDataService()).startBackground();
    }
}
