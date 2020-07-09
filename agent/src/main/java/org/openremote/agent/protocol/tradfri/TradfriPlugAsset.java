package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.device.event.PlugChangeOnEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.attribute.AttributeValueType.BOOLEAN;
import static org.openremote.model.attribute.MetaItemType.*;

public class TradfriPlugAsset extends TradfriAsset {

    /**
     * Construct the TradfriPlugAsset class
     * @param parentId the parent id.
     * @param agentLink the agent link.
     * @param plug the plug.
     * @param assetService the asset service.
     */
    public TradfriPlugAsset(String parentId, MetaItem agentLink, Plug plug, ProtocolAssetService assetService) {
        super(parentId, plug, AssetType.THING, assetService, agentLink);
    }

    /**
     * Method to create the asset attributes
     */
    @Override
    public void createAssetAttributes() {
        AssetAttribute plugStatus = new AssetAttribute("plugStatus", BOOLEAN, Values.create(false));
        plugStatus.addMeta(
                new MetaItem(LABEL, Values.create("Plug Status")),
                new MetaItem(DESCRIPTION, Values.create("The state of the TRÅDFRI plug (Checked means on, unchecked means off)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                new MetaItem(READ_ONLY, Values.create(false)),
                new MetaItem(RULE_STATE, Values.create(true)),
                new MetaItem(STORE_DATA_POINTS, Values.create(true)),
                agentLink
        );
        setAttributes(plugStatus);
    }

    /**
     * Method to create the event handlers
     */
    @Override
    public void createEventHandlers() {
        Asset asset = this;
        EventHandler<PlugChangeOnEvent> plugOnOffEventHandler = new EventHandler<PlugChangeOnEvent>() {
            @Override
            public void handle(PlugChangeOnEvent event) {
                Optional<AssetAttribute> plugStatus = getAttribute("plugStatus");
                Plug plug = device.toPlug();
                if(plugStatus.isPresent() && plug.getOn() != null) plugStatus.get().setValue(Values.create(plug.getOn()));
                assetService.mergeAsset(asset);
            }
        };
        device.addEventHandler(plugOnOffEventHandler);
    }

    /**
     * Method to set the initial values
     */
    @Override
    public void setInitialValues() {
        Plug plug = device.toPlug();
        Optional<AssetAttribute> plugStatus = getAttribute("plugStatus");
        if(plugStatus.isPresent() && plug.getOn() != null) plugStatus.get().setValue(Values.create(plug.getOn()));
    }
}
