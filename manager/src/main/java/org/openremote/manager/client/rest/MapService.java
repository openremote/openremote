package org.openremote.manager.client.rest;

import elemental.json.JsonObject;
import org.openremote.manager.shared.rest.MapRestService;

import javax.ws.rs.core.Response;

// TODO: Ideally this class needs to be generated automatically from the JAX-RS interface
public class MapService implements MapRestService {
    @Override
    public JsonObject getOptions() {
        return null;
    }

    @Override
    public Response getTile(int zoom, int column, int row) throws Exception {
        // This method is only used by Mapbox so we ignore it
        return null;
    }
}
