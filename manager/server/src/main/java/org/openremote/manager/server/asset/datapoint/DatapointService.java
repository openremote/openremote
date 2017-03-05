package org.openremote.manager.server.asset.datapoint;

import java.util.logging.Logger;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeValueChange;

import elemental.json.JsonValue;

/**
 * Store and retrieve datapoints 
 */
public class DatapointService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(DatapointService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop(Container container) throws Exception {
        // TODO Auto-generated method stub
        
    }

    public void addDatapoint(AttributeValueChange attributeValueChange) {
        AttributeRef attributeRef = attributeValueChange.getAttributeRef();
        String attributeName = attributeRef.getAttributeName();
        String assetId = attributeRef.getEntityId();
        JsonValue value = attributeValueChange.getValue();
        
    }

}
