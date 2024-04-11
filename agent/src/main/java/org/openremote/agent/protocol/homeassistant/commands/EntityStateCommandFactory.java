package org.openremote.agent.protocol.homeassistant.commands;

import org.openremote.agent.protocol.homeassistant.HomeAssistantEntityProcessor;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeRef;

import java.util.Optional;

import static org.openremote.agent.protocol.homeassistant.entities.HomeAssistantEntityType.*;

public final class EntityStateCommandFactory {

    public static Optional<EntityStateCommand> createEntityStateCommand(Asset<?> asset, AttributeRef attribute, String value) {

        var entityId = asset.getAttribute("HomeAssistantEntityId");
        if (entityId.isEmpty() || entityId.get().getValue().isEmpty())
        {
            return Optional.empty();
        }
        var entityIdString = (String) entityId.get().getValue().get();

        var entityType = HomeAssistantEntityProcessor.getEntityTypeFromEntityId(entityIdString);
        var attributeName = attribute.getName();
        var isStateAttribute = attributeName.equals("state");

        //if brightness convert value from 1-100 to 0-255
        if (attributeName.equals("brightness")) {
            value = String.valueOf((int) Math.round(Integer.parseInt(value) * 2.55));
        }
        
        return switch (entityType) {
            case ENTITY_TYPE_LIGHT, ENTITY_TYPE_SWITCH, ENTITY_TYPE_FAN -> {
                if (isStateAttribute) {
                    yield Optional.of(new EntityStateCommand("toggle", entityIdString, null, null));
                }
                yield Optional.of(new EntityStateCommand("turn_on", entityIdString, attributeName, value));
            }
            default -> Optional.empty();
        };

    }


}
