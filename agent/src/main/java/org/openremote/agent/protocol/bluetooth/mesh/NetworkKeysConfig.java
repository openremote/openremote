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

