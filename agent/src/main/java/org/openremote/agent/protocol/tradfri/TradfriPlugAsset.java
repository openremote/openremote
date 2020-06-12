package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.device.event.PlugChangeOnEvent;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.HashMap;
import java.util.Optional;

import static org.openremote.model.attribute.AttributeValueType.BOOLEAN;
import static org.openremote.model.attribute.MetaItemType.*;

public class TradfriPlugAsset extends TradfriAsset {

    /**
     * Construct the TradfriPlugAsset class
     */
    public TradfriPlugAsset(AssetAttribute protocolConfiguration, MetaItem agentLink, ProtocolAssetService assetService, Plug plug, HashMap<String, Device> tradfriDevices) {
        super(protocolConfiguration, agentLink, assetService, plug, AssetType.THING, tradfriDevices);
    }

    @Override
    public void createAssetAttributes() {
        asset.setAttributes(
                new AssetAttribute("plugOnOrOff", BOOLEAN, Values.create(false))
        );
        Optional<AssetAttribute> plugOnOrOff = asset.getAttribute("plugOnOrOff");
        if (plugOnOrOff.isPresent()) plugOnOrOff.get().setMeta(
                new MetaItem(LABEL, Values.create("State (on/off)")),
                new MetaItem(DESCRIPTION, Values.create("The state of the TRÅDFRI plug (Checked means on, unchecked means off)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                agentLink
        );
    }

    @Override
    public void createEventHandlers() {
        EventHandler<PlugChangeOnEvent> plugOnOffEventHandler = new EventHandler<PlugChangeOnEvent>() {
            @Override
            public void handle(PlugChangeOnEvent event) {
                Optional<AssetAttribute> plugOnOrOff = asset.getAttribute("plugOnOrOff");
                if (plugOnOrOff.isPresent()) plugOnOrOff.get().setValue(Values.create(device.toPlug().getOn()));
                Optional<String> assetId = protocolConfiguration.getAssetId();
                if (assetId.isPresent()) asset.setRealm(assetService.findAsset(assetId.get()).getRealm());
                assetService.mergeAsset(asset);
            }
        };
        device.addEventHandler(plugOnOffEventHandler);
    }

    @Override
    public void setInitialValues() {
        Optional<AssetAttribute> plugOnOrOff = asset.getAttribute("plugOnOrOff");
        if (plugOnOrOff.isPresent()) plugOnOrOff.get().setValue(Values.create(device.toPlug().getOn()));
    }
}
