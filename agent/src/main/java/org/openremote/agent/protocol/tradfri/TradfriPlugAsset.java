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
     * The agentlink.
     */
    private final MetaItem agentLink;

    /**
     * Construct the TradfriPlugAsset class
     */
    public TradfriPlugAsset(String parentId, MetaItem agentLink, Plug plug, ProtocolAssetService assetService) {
        super(parentId, plug, AssetType.THING, assetService);
        this.agentLink = agentLink;
    }

    @Override
    public void createAssetAttributes() {
        AssetAttribute plugOnOrOff = new AssetAttribute("plugOnOrOff", BOOLEAN, Values.create(false));
        plugOnOrOff.setMeta(
                new MetaItem(LABEL, Values.create("State (on/off)")),
                new MetaItem(DESCRIPTION, Values.create("The state of the TRÅDFRI plug (Checked means on, unchecked means off)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                agentLink
        );
        setAttributes(plugOnOrOff);
    }

    @Override
    public void createEventHandlers() {
        Asset asset = this;
        EventHandler<PlugChangeOnEvent> plugOnOffEventHandler = new EventHandler<PlugChangeOnEvent>() {
            @Override
            public void handle(PlugChangeOnEvent event) {
                Optional<AssetAttribute> plugOnOrOff = getAttribute("plugOnOrOff");
                plugOnOrOff.ifPresent(assetAttribute -> assetAttribute.setValue(Values.create(device.toPlug().getOn())));
                assetService.mergeAsset(asset);
            }
        };
        device.addEventHandler(plugOnOffEventHandler);
    }

    @Override
    public void setInitialValues() {
        Optional<AssetAttribute> plugOnOrOff = getAttribute("plugOnOrOff");
        plugOnOrOff.ifPresent(assetAttribute -> assetAttribute.setValue(Values.create(device.toPlug().getOn())));
    }
}
