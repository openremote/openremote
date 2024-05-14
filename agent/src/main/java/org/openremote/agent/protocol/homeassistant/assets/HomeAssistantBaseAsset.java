package org.openremote.agent.protocol.homeassistant.assets;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class HomeAssistantBaseAsset extends Asset<HomeAssistantBaseAsset> {

    public static AssetDescriptor<HomeAssistantBaseAsset> DESCRIPTOR = new AssetDescriptor<>("cube-outline", "03a6f0", HomeAssistantBaseAsset.class);

    public static final AttributeDescriptor<String> ENTITY_ID = new AttributeDescriptor<>("HomeAssistantEntityId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AttributeDescriptor<String> AGENT_ID = new AttributeDescriptor<>("AgentId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AttributeDescriptor<String> GROUP_FOR_ENTITY_TYPE = new AttributeDescriptor<>("GroupForEntityType", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));

    protected HomeAssistantBaseAsset() {
    }

    public HomeAssistantBaseAsset(String name, String entityId) {
        super(name);
        setEntityId(entityId);

    }

    public String getAgentId() {
        return getAttributes().getValue(AGENT_ID).orElseThrow();
    }

    public void setAgentId(String agentId) {
        getAttributes().setValue(AGENT_ID, agentId);
    }

    public String getGroupForEntityType() {
        return getAttributes().getValue(GROUP_FOR_ENTITY_TYPE).orElseThrow();
    }

    public void setGroupForEntityType(String groupForEntityType) {
        getAttributes().setValue(GROUP_FOR_ENTITY_TYPE, groupForEntityType);
    }

    public String getEntityId() {
        return getAttributes().getValue(ENTITY_ID).orElseThrow();
    }

    public void setEntityId(String entityId) {
        getAttributes().setValue(ENTITY_ID, entityId);
    }

}
