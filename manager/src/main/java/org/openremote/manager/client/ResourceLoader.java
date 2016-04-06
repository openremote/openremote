package org.openremote.manager.client;

import com.google.inject.Inject;
import org.openremote.manager.client.app.AppResources;

public class ResourceLoader {

    @Inject
    public ResourceLoader(AppResources appResources) {
        // appResources.todo().ensureInjected();
    }
}
