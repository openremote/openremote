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
package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.List;

/**
 * Contains the configuration required when exporting a selected number of Network Keys in a mesh network.
 */
public class NetworkKeysConfig extends ExportConfig {

    NetworkKeysConfig(final Builder config) {
        super(config);
    }

    /**
     * Use this class to configure when exporting all the Network Keys.
     */
    public static class ExportAll implements Builder {
        @Override
        public NetworkKeysConfig build() {
            return new NetworkKeysConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting some of the Network Keys in network.
     */
    public static class ExportSome implements Builder {

        private final List<NetworkKey> keys;

        /**
         * Constructs ExportSome to export only a selected number of network keys when exporting a mesh network.
         *
         * @param keys List of Network Keys to export.
         * @throws IllegalArgumentException if the list does not contain at least one network key.
         */
        public ExportSome(final List<NetworkKey> keys) {
            if (keys.isEmpty())
                throw new IllegalArgumentException("Error, at least one Network Key must be selected!");
            this.keys = keys;
        }

        protected List<NetworkKey> getKeys() {
            return keys;
        }

        @Override
        public NetworkKeysConfig build() {
            return new NetworkKeysConfig(this);
        }
    }
}

