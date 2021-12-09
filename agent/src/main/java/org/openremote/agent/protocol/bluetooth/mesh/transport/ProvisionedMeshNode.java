/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.Features;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.NodeKey;
import org.openremote.agent.protocol.bluetooth.mesh.Provisioner;
import org.openremote.agent.protocol.bluetooth.mesh.models.ConfigurationServerModel;
import org.openremote.agent.protocol.bluetooth.mesh.models.SigModelParser;
import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class ProvisionedMeshNode extends ProvisionedBaseMeshNode {

    public static final Logger LOG = Logger.getLogger(ProvisionedMeshNode.class.getName());

    public ProvisionedMeshNode() {
    }

    /**
     * Constructor to be used only by hte library
     *
     * @param node {@link UnprovisionedMeshNode}
     */
    public ProvisionedMeshNode(final UnprovisionedMeshNode node) {
        uuid = node.getDeviceUuid().toString();
        isConfigured = node.isConfigured();
        nodeName = node.getNodeName();
        mAddedNetKeys.add(new NodeKey(node.getKeyIndex()));
        mFlags = node.getFlags();
        unicastAddress = node.getUnicastAddress();
        deviceKey = node.getDeviceKey();
        ttl = node.getTtl();
        final NetworkKey networkKey = new NetworkKey(node.getKeyIndex(), node.getNetworkKey());
        mTimeStampInMillis = node.getTimeStamp();
    }

    /**
     * Constructor to be used only by the library
     *
     * @param provisioner {@link Provisioner}
     * @param netKeys     List of {@link NetworkKey}
     * @param appKeys     List of {@link ApplicationKey}
     */
    public ProvisionedMeshNode(final Provisioner provisioner,
                               final List<NetworkKey> netKeys,
                               final List<ApplicationKey> appKeys) {
        this.meshUuid = provisioner.getMeshUuid();
        uuid = provisioner.getProvisionerUuid();
        isConfigured = true;
        nodeName = provisioner.getProvisionerName();
        for (NetworkKey key : netKeys) {
            mAddedNetKeys.add(new NodeKey(key.getKeyIndex(), false));
        }
        for (ApplicationKey key : appKeys) {
            mAddedAppKeys.add(new NodeKey(key.getKeyIndex(), false));
        }
        if (provisioner.getProvisionerAddress() != null)
            unicastAddress = provisioner.getProvisionerAddress();
        sequenceNumber = 0;
        deviceKey = SecureUtils.generateRandomNumber();
        ttl = provisioner.getGlobalTtl();
        mTimeStampInMillis = System.currentTimeMillis();
        final MeshModel model = SigModelParser.getSigModel(SigModelParser.CONFIGURATION_CLIENT);
        final HashMap<Integer, MeshModel> models = new HashMap<>();
        models.put(model.getModelId(), model);
        final Element element = new Element(unicastAddress, 0, models);
        final HashMap<Integer, Element> elements = new HashMap<>();
        elements.put(unicastAddress, element);
        mElements = elements;
        nodeFeatures = new Features(Features.UNSUPPORTED, Features.UNSUPPORTED, Features.UNSUPPORTED, Features.UNSUPPORTED);
    }

    public final Map<Integer, Element> getElements() {
        return mElements;
    }

    /**
     * Check if an unicast address is the address of an element
     *
     * @param unicastAddress the address to check
     * @return if this address is the address of an element
     */
    public final boolean hasUnicastAddress(final int unicastAddress) {
        if (unicastAddress == getUnicastAddress())
            return true;
        for (Element element : mElements.values()) {
            if (element.getElementAddress() == unicastAddress)
                return true;
        }
        return false;
    }

    public final void setElements(final Map<Integer, Element> elements) {
        mElements = elements;
    }

    public final byte[] getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(final byte[] deviceKey) {
        this.deviceKey = deviceKey;
    }

    public final int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Sets the sequence number
     * <p>
     * This is only meant to be used internally within the library.
     * However this is open now for users to set the sequence number manually in provisioner node.
     * </p>
     *
     * @param sequenceNumber sequence number of the node
     */
    public final void setSequenceNumber(final int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public final Integer getCompanyIdentifier() {
        return companyIdentifier;
    }

    public final void setCompanyIdentifier(final Integer companyIdentifier) {
        this.companyIdentifier = companyIdentifier;
    }

    public final Integer getProductIdentifier() {
        return productIdentifier;
    }

    public final void setProductIdentifier(final Integer productIdentifier) {
        this.productIdentifier = productIdentifier;
    }

    public final Integer getVersionIdentifier() {
        return versionIdentifier;
    }

    public final void setVersionIdentifier(final Integer versionIdentifier) {
        this.versionIdentifier = versionIdentifier;
    }

    public final Integer getCrpl() {
        return crpl;
    }

    public final void setCrpl(final Integer crpl) {
        this.crpl = crpl;
    }

    /**
     * Returns the {@link Features} of the node
     */
    public final Features getNodeFeatures() {
        return nodeFeatures;
    }

    /**
     * Set {@link Features} of the node
     *
     * @param features feature set supported by the node
     */
    public final void setNodeFeatures(final Features features) {
        this.nodeFeatures = features;
    }

    /**
     * Returns the list of Network keys added to this node
     */
    public final List<NodeKey> getAddedNetKeys() {
        return Collections.unmodifiableList(mAddedNetKeys);
    }

    public final void setAddedNetKeys(final List<NodeKey> addedNetKeyIndexes) {
        mAddedNetKeys = addedNetKeyIndexes;
    }

    /**
     * Adds a NetKey index that was added to the node
     *
     * @param index NetKey index
     */
    protected final void setAddedNetKeyIndex(final int index) {
        if (!MeshParserUtils.isNodeKeyExists(mAddedNetKeys, index)) {
            mAddedNetKeys.add(new NodeKey(index));
        }
    }

    /**
     * Update a net key's updated state
     *
     * @param index NetKey index
     */
    protected final void updateAddedNetKey(final int index) {
        final NodeKey nodeKey = MeshParserUtils.getNodeKey(mAddedNetKeys, index);
        if (nodeKey != null) {
            nodeKey.setUpdated(true);
        }
    }

    /**
     * Update the added net key list of the node
     *
     * @param indexes NetKey index
     */
    protected final void updateNetKeyList(final List<Integer> indexes) {
        mAddedNetKeys.clear();
        for (Integer index : indexes) {
            mAddedNetKeys.add(new NodeKey(index, false));
        }
    }

    /**
     * Removes an NetKey index that was added to the node
     *
     * @param index NetKey index
     */
    protected final void removeAddedNetKeyIndex(final int index) {
        for (int i = 0; i < mAddedNetKeys.size(); i++) {
            final int keyIndex = mAddedNetKeys.get(i).getIndex();
            if (keyIndex == index) {
                mAddedNetKeys.remove(i);
                for (Element element : mElements.values()) {
                    for (MeshModel model : element.getMeshModels().values()) {
                        if (model.getModelId() == SigModelParser.CONFIGURATION_SERVER) {
                            final ConfigurationServerModel configServerModel = (ConfigurationServerModel) model;
                            if (configServerModel.getHeartbeatPublication() != null &&
                                configServerModel.getHeartbeatPublication().getNetKeyIndex() == index) {
                                configServerModel.setHeartbeatPublication(null);
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Returns the list of added AppKey indexes to the node
     */
    public final List<NodeKey> getAddedAppKeys() {
        return mAddedAppKeys;
    }

    public final void setAddedAppKeys(final List<NodeKey> addedAppKeyIndexes) {
        mAddedAppKeys = addedAppKeyIndexes;
    }

    /**
     * Adds an AppKey index that was added to the node
     *
     * @param index AppKey index
     */
    protected final void setAddedAppKeyIndex(final int index) {
        if (!MeshParserUtils.isNodeKeyExists(mAddedAppKeys, index)) {
            this.mAddedAppKeys.add(new NodeKey(index));
        }
    }

    /**
     * Update an app key's updated state
     *
     * @param index AppKey index
     */
    protected final void updateAddedAppKey(final int index) {
        final NodeKey nodeKey = MeshParserUtils.getNodeKey(mAddedNetKeys, index);
        if (nodeKey != null) {
            nodeKey.setUpdated(true);
        }
    }

    /**
     * Update the added net key list of the node
     *
     * @param netKeyIndex NetKey Index
     * @param indexes     AppKey indexes
     */
    protected final void updateAppKeyList(final int netKeyIndex, final List<Integer> indexes, final List<ApplicationKey> keyIndexes) {
        if (mAddedAppKeys.isEmpty()) {
            mAddedAppKeys.addAll(addAppKeyList(indexes, new ArrayList<>()));
        } else {
            final ArrayList<NodeKey> tempList = new ArrayList<>(mAddedAppKeys);
            for (ApplicationKey applicationKey : keyIndexes) {
                if (applicationKey.getBoundNetKeyIndex() == netKeyIndex) {
                    for (NodeKey nodeKey : mAddedAppKeys) {
                        if (nodeKey.getIndex() == applicationKey.getKeyIndex()) {
                            tempList.remove(nodeKey);
                        }
                    }
                }
            }
            mAddedAppKeys.clear();
            addAppKeyList(indexes, tempList);
            mAddedAppKeys.addAll(tempList);
        }
    }

    private List<NodeKey> addAppKeyList(final List<Integer> indexes, final ArrayList<NodeKey> tempList) {
        for (Integer index : indexes) {
            tempList.add(new NodeKey(index, false));
        }
        return tempList;
    }

    /**
     * Removes an AppKey index that was added to the node
     *
     * @param index AppKey index
     */
    protected final void removeAddedAppKeyIndex(final int index) {
        for (int i = 0; i < mAddedAppKeys.size(); i++) {
            final int keyIndex = mAddedAppKeys.get(i).getIndex();
            if (keyIndex == index) {
                mAddedAppKeys.remove(i);
                for (Map.Entry<Integer, Element> elementEntry : getElements().entrySet()) {
                    final Element element = elementEntry.getValue();
                    for (Map.Entry<Integer, MeshModel> modelEntry : element.getMeshModels().entrySet()) {
                        final MeshModel model = modelEntry.getValue();
                        if (model != null) {
                            for (int j = 0; j < model.getBoundAppKeyIndexes().size(); j++) {
                                final int boundKeyIndex = model.getBoundAppKeyIndexes().get(j);
                                if (boundKeyIndex == index) {
                                    model.mBoundAppKeyIndexes.remove(j);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Sets the data from the {@link ConfigCompositionDataStatus}
     *
     * @param configCompositionDataStatus Composition data status object
     */
    protected final void setCompositionData(final ConfigCompositionDataStatus configCompositionDataStatus) {
        companyIdentifier = configCompositionDataStatus.getCompanyIdentifier();
        productIdentifier = configCompositionDataStatus.getProductIdentifier();
        versionIdentifier = configCompositionDataStatus.getVersionIdentifier();
        crpl = configCompositionDataStatus.getCrpl();
        final boolean relayFeatureSupported = configCompositionDataStatus.isRelayFeatureSupported();
        final boolean proxyFeatureSupported = configCompositionDataStatus.isProxyFeatureSupported();
        final boolean friendFeatureSupported = configCompositionDataStatus.isFriendFeatureSupported();
        final boolean lowPowerFeatureSupported = configCompositionDataStatus.isLowPowerFeatureSupported();
        nodeFeatures = new Features(friendFeatureSupported ? Features.ENABLED : Features.UNSUPPORTED,
            lowPowerFeatureSupported ? Features.ENABLED : Features.UNSUPPORTED,
            proxyFeatureSupported ? Features.ENABLED : Features.UNSUPPORTED,
            relayFeatureSupported ? Features.ENABLED : Features.UNSUPPORTED);
        mElements.putAll(configCompositionDataStatus.getElements());
    }

    private int getFeatureState(final Boolean feature) {
        if (feature != null && feature) {
            return 2;
        }
        return 0;
    }

    /**
     * Sets the bound app key data from the {@link ConfigModelAppStatus}
     *
     * @param configModelAppStatus ConfigModelAppStatus containing the bound app key information
     */
    protected final void setAppKeyBindStatus(
        final ConfigModelAppStatus configModelAppStatus) {
        if (configModelAppStatus.isSuccessful()) {
            final Element element = mElements.get(configModelAppStatus.getElementAddress());
            if (element != null) {
                final int modelIdentifier = configModelAppStatus.getModelIdentifier();
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                if (model != null) {
                    final int appKeyIndex = configModelAppStatus.getAppKeyIndex();
                    model.setBoundAppKeyIndex(appKeyIndex);
                }
            }
        }
    }

    /**
     * Sets the unbind app key data from the {@link ConfigModelAppStatus}
     *
     * @param configModelAppStatus ConfigModelAppStatus containing the unbound app key information
     */
    protected final void setAppKeyUnbindStatus(
        final ConfigModelAppStatus configModelAppStatus) {
        if (configModelAppStatus.isSuccessful()) {
            final Element element = mElements.get(configModelAppStatus.getElementAddress());
            if (element != null) {
                final int modelIdentifier = configModelAppStatus.getModelIdentifier();
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                final int appKeyIndex = configModelAppStatus.getAppKeyIndex();
                if (model != null) {
                    model.removeBoundAppKeyIndex(appKeyIndex);
                }
            }
        }
    }

    private void sortElements(final Map<Integer, Element> unorderedElements) {
        final Set<Integer> unorderedKeys = unorderedElements.keySet();

        final List<Integer> orderedKeys = new ArrayList<>(unorderedKeys);
        Collections.sort(orderedKeys);
        for (int key : orderedKeys) {
            mElements.put(key, unorderedElements.get(key));
        }
    }

    void setSeqAuth(final int src, final int seqAuth) {
        mSeqAuth.put(src, seqAuth);
    }

    public Integer getSeqAuth(final int src) {
        if (mSeqAuth.size() == 0) {
            return null;
        }

        return mSeqAuth.get(src);
    }

    public boolean isExist(final int modelId) {
        for (Map.Entry<Integer, Element> elementEntry : mElements.entrySet()) {
            final Element element = elementEntry.getValue();
            for (Map.Entry<Integer, MeshModel> modelEntry : element.getMeshModels().entrySet()) {
                final MeshModel model = modelEntry.getValue();
                if (model != null && model.getModelId() == modelId) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Increments the sequence number
     */
    public int incrementSequenceNumber() {
        return sequenceNumber = sequenceNumber + 1;
    }


}
