package org.openremote.test.map;

import elemental.json.JsonObject;
import org.junit.Test;
import org.openremote.container.ContainerService;
import org.openremote.manager.shared.rest.MapResource;
import org.openremote.test.ServerTest;

import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

public class MapResourceTest extends ServerTest {

    private static final Logger LOG = Logger.getLogger(MapResourceTest.class.getName());

    @Override
    protected ContainerService[] getContainerServices() {
        return new ContainerService[] {
            new org.openremote.manager.server.map.MapService()
        };
    }

    @Test
    public void getMapSettings() throws Exception{
        MapResource mapResource = getTargetResource(MapResource.class);
        JsonObject mapSettings = mapResource.getSettings(null);
        assertNotNull(mapSettings);
    }

    // TODO This is just testing propert start/shutdown of container
    @Test
    public void getMapSettings2() throws Exception{
        MapResource mapResource = getTargetResource(MapResource.class);
        JsonObject mapSettings = mapResource.getSettings(null);
        assertNotNull(mapSettings);
    }

}
