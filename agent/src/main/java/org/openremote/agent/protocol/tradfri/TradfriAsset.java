package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;

/**
 * Abstract class for TRÅDFRI assets
 */
public abstract class TradfriAsset extends Asset {

    /**
     * The device.
     */
    protected Device device;

    /**
     * The asset service.
     */
    protected ProtocolAssetService assetService;

    /**
     * The agent link.
     */
    protected MetaItem agentLink;

    /**
     * Construct the TradfriAsset class.
     * @param parentId the parent id.
     * @param device the device.
     * @param assetType the asset type.
     * @param assetService the asset service.
     */
    public TradfriAsset(String parentId, Device device, AssetType assetType, ProtocolAssetService assetService, MetaItem agentLink) {
        super(device.getName() != null && !device.getName().isEmpty() ? device.getName() : "Unnamed " + device.getInstanceId(), assetType);
        this.device = device;
        this.assetService = assetService;
        this.agentLink = agentLink;
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
