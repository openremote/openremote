/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol;

import org.openremote.model.PersistenceEvent;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.query.AssetQuery;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Interface for protocols to perform limited asset related operations.
 */
public interface ProtocolAssetService extends ContainerService {

    /**
     * Options when merging and storing assets from protocols.
     */
    class MergeOptions {

        final protected String assignToUserName;
        final protected Predicate<String> attributeNamesToEvaluate;
        final protected Predicate<String> ignoredAttributeNames;
        final protected Predicate<String> ignoredAttributeKeys;

        public MergeOptions(String assignToUserName) {
            this(assignToUserName, null, null, null);
        }

        public MergeOptions(Predicate<String> attributeNamesToEvaluate) {
            this(null, attributeNamesToEvaluate, null, null);
        }

        public MergeOptions(Predicate<String> ignoredAttributeNames, Predicate<String> ignoredAttributeKeys) {
            this(null, null, ignoredAttributeNames, ignoredAttributeKeys);
        }

        public MergeOptions(String assignToUserName, Predicate<String> ignoredAttributeKeys) {
            this(assignToUserName, null, null, ignoredAttributeKeys);
        }

        public MergeOptions(String assignToUserName, Predicate<String> attributeNamesToEvaluate, Predicate<String> ignoredAttributeNames, Predicate<String> ignoredAttributeKeys) {
            this.assignToUserName = assignToUserName;
            this.attributeNamesToEvaluate = attributeNamesToEvaluate;
            this.ignoredAttributeNames = ignoredAttributeNames;
            this.ignoredAttributeKeys = ignoredAttributeKeys;
        }

        /**
         * Assigns the merged asset to the given user, can be <code>null</code> to not assign the asset to a user. The
         * {@link #mergeAsset} call returns <code>null</code> if the user doesn't exist or the asset couldn't be
         * assigned.
         */
        public String getAssignToUserName() {
            return assignToUserName;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the attributes in this
         * predicate have change, then perform the merge on storage. Ignores what {#link getIgnoredAttributeNames} has
         * stored if attributeNamesToEvaluate is not null.
         */
        public Predicate<String> getAttributeNamesToEvaluate() {
            return attributeNamesToEvaluate;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the ignored attributes
         * have changed, don't perform the merge on storage.
         */
        public Predicate<String> getIgnoredAttributeNames() {
            return ignoredAttributeNames;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the ignored keys of
         * any attributes have changed, don't perform the merge on storage. If {#link getAttributeNamesToEvaluate} is
         * not null, then ignoredAttributeKeys is ignored.
         */
        public Predicate<String> getIgnoredAttributeKeys() {
            return ignoredAttributeKeys;
        }
    }

    /**
     * Protocols may store assets in the context or update existing assets. A unique identifier must be set by the
     * protocol implementor, and the parent must be the {@link Agent} associated with the requesting protocol instance.
     * This operation stores transient or detached state and returns the current state. It will override any existing
     * stored asset data, ignoring versions.
     */
    <T extends Asset<?>> T mergeAsset(T asset);

    /**
     * Protocols may remove assets from the context store.
     *
     * @return <code>false</code> if the delete could not be performed (asset may have children?)
     */
    boolean deleteAssets(String...assetIds);

    /**
     * Get asset of specified type from the store by ID.
     */
    <T extends Asset<?>> T findAsset(String assetId, Class<T> assetType);

    /**
     * Get asset from the store by ID.
     */
    <T extends Asset<?>> T findAsset(String assetId);

    /**
     * Get assets by an {@link org.openremote.model.query.AssetQuery}; can only access {@link Asset}s that are
     * descendants of the specified {@link Asset}
     */
    List<Asset<?>> findAssets(String assetId, AssetQuery assetQuery);

    /**
     * Protocols can send arbitrary attribute change events for regular processing.
     */
    void sendAttributeEvent(AttributeEvent attributeEvent);

    /**
     * Subscribe to changes of {@link Asset}s that are descendants of the specified agent.
     * <p>
     * When an agent is unlinked from a protocol then all subscriptions will be automatically removed also; it is safe
     * to call this method multiple times for the same agentId and assetChangeConsumer and only a single subscription
     * would actually be created.
     */
    void subscribeChildAssetChange(String agentId, Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer);

    /**
     * Unsubscribe from asset changes for the specified agent.
     * <p>
     * When an agent is unlinked from a protocol then all subscriptions will be automatically removed also.
     */
    void unsubscribeChildAssetChange(String agentId, Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer);
}
