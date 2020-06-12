package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;

import java.util.HashMap;

public abstract class TradfriAsset {
    private String name;
    protected Asset asset;
    protected AssetAttribute protocolConfiguration;
    protected MetaItem agentLink;
    protected ProtocolAssetService assetService;
    protected Device device;

    public TradfriAsset(AssetAttribute protocolConfiguration, MetaItem agentLink, ProtocolAssetService assetService, Device device, AssetType assetType, HashMap<String, Device> tradfriDevices) {
        this.name = UniqueIdentifierGenerator.generateId("tradfri_" + device.getInstanceId());
        this.asset = new Asset(device.getName(), assetType);
        this.protocolConfiguration = protocolConfiguration;
        this.agentLink = agentLink;
        this.assetService = assetService;
        this.device = device;
        this.setIds();
        this.createAssetAttributes();
        this.setInitialValues();
        this.createEventHandlers();
        device.enableObserve();
        this.assetService.mergeAsset(this.asset);
        tradfriDevices.put(asset.getId(), device);
    }

    /**
     * Method to set the id and parent id of the asset
     */
    public void setIds() {
        asset.setId(name);
        if (!protocolConfiguration.getAssetId().isPresent()) {
            return;
        }
        asset.setParentId(protocolConfiguration.getAssetId().get());
    }

    /**
     * Method to create the asset attributes
     */
    public abstract void createAssetAttributes();

    /**
     * Method to create the event handlers
     */
    public abstract void createEventHandlers();

    /**
     * Method to set the initial values
     */
    public abstract void setInitialValues();
}
