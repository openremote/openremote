package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;

public abstract class TradfriAsset extends Asset {

    protected Device device;
    protected ProtocolAssetService assetService;

    public TradfriAsset(String parentId, Device device, AssetType assetType, ProtocolAssetService assetService) {
        super(device.getName(), assetType);
        this.device = device;
        this.assetService = assetService;
        if(parentId != null && !parentId.isEmpty()){
            setParentId(parentId);
            setRealm(assetService.findAsset(parentId).getRealm());
        }
        setId(UniqueIdentifierGenerator.generateId("tradfri_" + device.getInstanceId()));
        this.createAssetAttributes();
        this.setInitialValues();
        this.createEventHandlers();
        device.enableObserve();
        assetService.mergeAsset(this);
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
