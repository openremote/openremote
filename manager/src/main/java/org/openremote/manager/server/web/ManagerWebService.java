package org.openremote.manager.server.web;

import org.openremote.container.Container;
import org.openremote.container.web.WebService;

import java.nio.file.Paths;

import static org.openremote.manager.server.Constants.MASTER_REALM;

public class ManagerWebService extends WebService {

    public static final String WEBSERVER_DOCROOT = "WEBSERVER_DOCROOT";
    public static final String WEBSERVER_DOCROOT_DEFAULT = "src/main/webapp";

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        setDefaultRealm(MASTER_REALM);
        setStaticResourceDocRoot(Paths.get(container.getConfig(WEBSERVER_DOCROOT, WEBSERVER_DOCROOT_DEFAULT)));
    }
}
