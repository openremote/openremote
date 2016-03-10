package org.openremote.manager.client;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules(
        {
                MainModule.class,
        }
)
public interface ManagerGinjector extends Ginjector {
    AppController getAppController();
}
