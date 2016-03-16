package org.openremote.manager.client;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import org.openremote.manager.client.app.AppController;

@GinModules(
        {
                MainModule.class,
        }
)
public interface ManagerGinjector extends Ginjector {
    AppController getAppController();
}
