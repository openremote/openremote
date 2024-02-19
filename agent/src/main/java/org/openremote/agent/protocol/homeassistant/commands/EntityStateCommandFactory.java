package org.openremote.agent.protocol.homeassistant.commands;

import org.openremote.agent.protocol.homeassistant.HomeAssistantEntityProcessor;
import org.openremote.agent.protocol.homeassistant.assets.HomeAssistantBaseAsset;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;

import java.util.Optional;

import static org.openremote.agent.protocol.homeassistant.entities.HomeAssistantEntityType.*;

public final class EntityStateCommandFactory {

    public static Optional<EntityStateCommand> createEntityStateCommand(Asset<?> asset, AttributeRef attribute, String value) {

        if (!(asset instanceof HomeAssistantBaseAsset homeAssistantAsset)) {
            return Optional.empty();
        }

        var entityId = homeAssistantAsset.getEntityId();
        var entityType = HomeAssistantEntityProcessor.getEntityTypeFromEntityId(entityId);
        var attributeName = attribute.getName();
        var isStateAttribute = attributeName.equals("state");

        // currently the only entity types that actually need to handle state changes are lights and switches.
        return switch (entityType) {
            case ENTITY_TYPE_LIGHT, ENTITY_TYPE_SWITCH, ENTITY_TYPE_FAN -> {
                if (isStateAttribute) {
                    yield Optional.of(new EntityStateCommand("toggle", entityId, null, null));
                }
                yield Optional.of(new EntityStateCommand("turn_on", entityId, attributeName, value));
            }
            default -> Optional.empty();
        };

    }


}
