package org.openremote.ccs;

import org.openremote.ccs.proxy.ProxyService;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerService;

public class Server {

    public static void main(String[] args) throws Exception {
        new Container(
            new MessageBrokerService(),
            new CCSWebService(),
            new CCSIdentityService(),
            new CCSPersistenceService(),
            new ProxyService(),
            new DemoDataService()
        ).startBackground();
    }

}
