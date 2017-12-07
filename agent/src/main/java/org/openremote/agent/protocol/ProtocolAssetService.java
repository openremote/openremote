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
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;

import java.util.Objects;

/**
 * Interface for protocols to perform limited asset related operations.
 */
public interface ProtocolAssetService extends ContainerService {

    /**
     * Options when merging and storing assets from protocols.
     */
    class MergeOptions {

        final protected String assignToUserName;
        final protected boolean ignoreAttributeValueTimestamps;

        public MergeOptions(String assignToUserName) {
            this(assignToUserName, false);
        }

        public MergeOptions(boolean ignoreAttributeValueTimestamps) {
            this(null, ignoreAttributeValueTimestamps);
        }

        public MergeOptions(String assignToUserName, boolean ignoreAttributeValueTimestamps) {
            this.assignToUserName = assignToUserName;
            this.ignoreAttributeValueTimestamps = ignoreAttributeValueTimestamps;
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
         * Compare existing and merged asset state before storing, if only the
         * {@link AbstractValueTimestampHolder#getValueTimestamp()}s have changed, don't perform the merge.
         */
        public boolean isIgnoreAttributeValueTimestamps() {
            return ignoreAttributeValueTimestamps;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MergeOptions that = (MergeOptions) o;
            return ignoreAttributeValueTimestamps == that.ignoreAttributeValueTimestamps &&
                Objects.equals(assignToUserName, that.assignToUserName);
        }

        @Override
        public int hashCode() {

            return Objects.hash(assignToUserName, ignoreAttributeValueTimestamps);
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
     * Protocols can send arbitrary attribute change events for regular processing.
     */
    void sendAttributeEvent(AttributeEvent attributeEvent);

}
