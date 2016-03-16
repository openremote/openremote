package org.openremote.manager.server;

import org.openremote.container.Container;
import org.openremote.manager.server.assets.AssetsService;
import org.openremote.manager.server.map.MapService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebService;

import java.util.logging.Logger;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) throws Exception {
        new Container(
            new ManagerWebService(),
            new ManagerIdentityService(),
            new AssetsService(),
            new MapService(),
            new SampleDataService()
        ).startBackground();
    }
}
