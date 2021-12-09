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

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base mesh model class
 * <p>
 * This class contains properties such as Model Identifier, bound keys, key indexes, subscription
 * and publication settings belonging to a mesh model.
 * </p>
 */
public abstract class MeshModel {

    protected int mModelId;

    final List<Integer> mBoundAppKeyIndexes = new ArrayList<>();

    final Map<Integer, String> mBoundAppKeys = new LinkedHashMap<>();

    final List<Integer> subscriptionAddresses = new ArrayList<>();

    final List<UUID> labelUuids = new ArrayList<>();

    PublicationSettings mPublicationSettings;
    protected int currentScene;
    protected int targetScene;

    protected List<Integer> sceneNumbers = new ArrayList<>();

    public MeshModel(final int modelId) {
        this.mModelId = modelId;
    }

    MeshModel() {
    }

    /**
     * Returns the 16-bit model id which could be a SIG Model or a Vendor Model
     *
     * @return modelId
     */
    public abstract int getModelId();

    /**
     * Returns the Bluetooth SIG defined model name
     *
     * @return model name
     */
    public abstract String getModelName();

    /**
     * Returns bound appkey indexes for this model
     */
    public List<Integer> getBoundAppKeyIndexes() {
        return Collections.unmodifiableList(mBoundAppKeyIndexes);
    }

    public void setBoundAppKeyIndex(final int appKeyIndex) {
        if (!mBoundAppKeyIndexes.contains(appKeyIndex))
            mBoundAppKeyIndexes.add(appKeyIndex);
    }

    public void setBoundAppKeyIndexes(final List<Integer> indexes) {
        mBoundAppKeyIndexes.clear();
        mBoundAppKeyIndexes.addAll(indexes);
    }

    @SuppressWarnings("RedundantCollectionOperation")
    public void removeBoundAppKeyIndex(final int appKeyIndex) {
        if (mBoundAppKeyIndexes.contains(appKeyIndex)) {
            final int position = mBoundAppKeyIndexes.indexOf(appKeyIndex);
            mBoundAppKeyIndexes.remove(position);
        }
    }

    /**
     * Returns the list of subscription addresses belonging to this model
     *
     * @return subscription addresses
     */
    public List<Integer> getSubscribedAddresses() {
        return Collections.unmodifiableList(subscriptionAddresses);
    }

    /**
     * Returns the list of label UUIDs subscribed to this model
     */
    public List<UUID> getLabelUUID() {
        return Collections.unmodifiableList(labelUuids);
    }

    /**
     * Returns the label UUID for a given virtual address
     *
     * @param address 16-bit virtual address
     */
    public UUID getLabelUUID(final int address) {
        return MeshAddress.getLabelUuid(labelUuids, address);
    }

    /**
     * Sets the data from the {@link ConfigModelPublicationStatus}
     *
     * @param status publication set status
     */
    protected void setPublicationStatus(final ConfigModelPublicationStatus status,
                                        final UUID labelUUID) {
        if (status.isSuccessful()) {
            if (!MeshAddress.isValidUnassignedAddress(status.getPublishAddress())) {
                mPublicationSettings = new PublicationSettings(status.getPublishAddress(),
                    labelUUID,
                    status.getAppKeyIndex(),
                    status.getCredentialFlag(),
                    status.getPublishTtl(),
                    status.getPublicationSteps(),
                    status.getPublicationResolution(),
                    status.getPublishRetransmitCount(),
                    status.getPublishRetransmitIntervalSteps());
            } else {
                mPublicationSettings = null;
            }
        }
    }

    /**
     * Updates the data from the {@link ConfigModelPublicationStatus}
     *
     * @param status publication set status
     */
    protected void updatePublicationStatus(final ConfigModelPublicationStatus status) {
        if (status.isSuccessful()) {
            if (mPublicationSettings != null) {
                mPublicationSettings.setPublishAddress(status.getPublishAddress());
                if (!MeshAddress.isValidVirtualAddress(status.getPublishAddress())) {
                    mPublicationSettings.setLabelUUID(null);
                }
                mPublicationSettings.setAppKeyIndex(status.getAppKeyIndex());
                mPublicationSettings.setCredentialFlag(status.getCredentialFlag());
                mPublicationSettings.setPublishTtl(status.getPublishTtl());
                mPublicationSettings.setPublicationSteps(status.getPublicationSteps());
                mPublicationSettings.setPublicationResolution(status.getPublicationResolution());
                mPublicationSettings.setPublishRetransmitCount(status.getPublishRetransmitCount());
                mPublicationSettings.setPublishRetransmitIntervalSteps(status.getPublishRetransmitIntervalSteps());
            }
        }
    }

    /**
     * Returns the publication settings used in this model
     *
     * @return publication settings
     */
    public PublicationSettings getPublicationSettings() {
        return mPublicationSettings;
    }

    public void setPublicationSettings(final PublicationSettings publicationSettings) {
        mPublicationSettings = publicationSettings;
    }

    /**
     * Sets the subscription address in a mesh model
     *
     * @param subscriptionAddress Subscription address
     */
    protected void addSubscriptionAddress(final int subscriptionAddress) {
        if (!subscriptionAddresses.contains(subscriptionAddress)) {
            subscriptionAddresses.add(subscriptionAddress);
        }
    }

    /**
     * Sets the subscription address in a mesh model
     *
     * @param labelUuid Label uuid of the the subscription address
     * @param address   Subscription address
     */
    protected void addSubscriptionAddress(final UUID labelUuid, final int address) {
        if (!labelUuids.contains(labelUuid)) {
            labelUuids.add(labelUuid);
        }

        if (!subscriptionAddresses.contains(address)) {
            subscriptionAddresses.add(address);
        }
    }

    /**
     * Removes the subscription address in a mesh model
     *
     * @param address Subscription address
     */
    protected void removeSubscriptionAddress(final Integer address) {
        subscriptionAddresses.remove(address);
    }

    /**
     * Removes the subscription address in a mesh model
     *
     * @param labelUuid Label UUID
     * @param address   Subscription address
     */
    protected void removeSubscriptionAddress(final UUID labelUuid,
                                             final Integer address) {
        labelUuids.remove(labelUuid);
        removeSubscriptionAddress(address);
    }

    /**
     * Removes all the subscription addresses in a mesh model
     */
    protected void removeAllSubscriptionAddresses() {
        labelUuids.clear();
        subscriptionAddresses.clear();
    }

    /**
     * Overwrites the subscription addresses in a mesh model by clearing the existing addresses and adding a new address
     *
     * @param subscriptionAddress Subscription address
     */
    protected void overwriteSubscriptionAddress(final Integer subscriptionAddress) {
        subscriptionAddresses.clear();
        addSubscriptionAddress(subscriptionAddress);
    }

    /**
     * Overwrites the subscription addresses in a mesh model by clearing the existing addresses and adding a new address
     *
     * @param labelUuid Label UUID
     * @param address   Subscription address
     */
    protected void overwriteSubscriptionAddress(final UUID labelUuid,
                                                final Integer address) {
        labelUuids.clear();
        addSubscriptionAddress(labelUuid, address);
        overwriteSubscriptionAddress(address);
    }

    /**
     * Update the subscription addresses list
     *
     * @param addresses List of subscription addresses
     */
    protected void updateSubscriptionAddressesList(final List<Integer> addresses) {
        subscriptionAddresses.clear();
        subscriptionAddresses.addAll(addresses);
    }
}
