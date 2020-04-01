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

import org.openremote.container.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueFilter;

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
         * Assigns the merged asset to the given user, can be <code>null</code> to not assign
         * the asset to a user. The {@link #mergeAsset} call returns <code>null</code> if the
         * user doesn't exist or the asset couldn't be assigned.
         */
        public String getAssignToUserName() {
            return assignToUserName;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the
         * attributes in this predicate have change, then perform the merge on storage. Ignores what {#link getIgnoredAttributeNames}
         * has stored if attributeNamesToEvaluate is not null.
         */
        public Predicate<String> getAttributeNamesToEvaluate() {
            return attributeNamesToEvaluate;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the
         * ignored attributes have changed, don't perform the merge on storage.
         */
        public Predicate<String> getIgnoredAttributeNames() {
            return ignoredAttributeNames;
        }

        /**
         * Compare existing and merged asset state before doing the actual storage merge. If only the
         * ignored keys of any attributes have changed, don't perform the merge on storage. If {#link getAttributeNamesToEvaluate}
         * is not null, then ignoredAttributeKeys is ignored.
         */
        public Predicate<String> getIgnoredAttributeKeys() {
            return ignoredAttributeKeys;
        }
    }

    /**
     * Protocols can update their own protocol configuration, for example, to store configuration
     * details such as temporary access (e.g. OAuth offline) tokens.
     */
    void updateProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Protocols may store assets in the context or update existing assets. A unique identifier
     * must be set by the protocol implementor, as well as a parent identifier. This operation
     * stores transient or detached state and returns the current state. It will override any
     * existing stored asset data, ignoring versions.
     */
    Asset mergeAsset(Asset asset);

    /**
     * Protocols may store assets in the context or update existing assets. A unique identifier
     * must be set by the protocol implementor, as well as a parent identifier. This operation
     * stores transient or detached state and returns the current state. It will override any
     * existing stored asset data, ignoring versions. This call may return <code>null</code>
     * if the desired {@link MergeOptions} were not successful.
     */
    Asset mergeAsset(Asset asset, MergeOptions options);

    /**
     * Protocols may remove assets from the context store.
     *
     * @return <code>false</code> if the delete could not be performed (asset may have children?)
     */
    boolean deleteAsset(String assetId);

    /**
     * Get asset from the store by ID.
     */
    Asset findAsset(String assetId);

    /**
     * Protocols can send arbitrary attribute change events for regular processing.
     */
    void sendAttributeEvent(AttributeEvent attributeEvent);

    /**
     * Gets the Agent {@link Asset} that the specified {@link org.openremote.model.asset.agent.ProtocolConfiguration}
     * belongs to.
     */
    Asset getAgent(AssetAttribute protocolConfiguration);

    /**
     * Apply the specified set of {@link ValueFilter}s to the specified {@link Value}
     */
    Value applyValueFilters(Value value, ValueFilter<?>... filters);
}
