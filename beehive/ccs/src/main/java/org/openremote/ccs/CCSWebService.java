package org.openremote.ccs;

import org.openremote.container.Container;
import org.openremote.container.web.WebService;

public class CCSWebService extends WebService {

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        setDefaultRealm(Constants.MASTER_REALM);
    }

    @Override
    public void configure(Container container) throws Exception {

        container.getService(WebService.class).getApiSingletons().add(
            new ControllerCommandResourceImpl(container.getService(CCSPersistenceService.class))
        );

        super.configure(container);
    }
}
