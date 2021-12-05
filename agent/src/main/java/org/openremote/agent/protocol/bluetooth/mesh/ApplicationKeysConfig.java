package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.List;

/**
 * Contains the configuration required when exporting a selected number of Application Keys in a mesh network.
 */
public class ApplicationKeysConfig extends ExportConfig {

    public static class ExportAll implements Builder {
        @Override
        public ApplicationKeysConfig build() {
            return new ApplicationKeysConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting all the Application Keys.
     */
    public static class ExportSome implements Builder {

        private final List<ApplicationKey> keys;

        /**
         * Constructs ExportSome to export only a selected number of Application Keys when exporting a mesh network.
         *
         * @param keys List of Application Keys to export.
         */
        public ExportSome(final List<ApplicationKey> keys) {
            this.keys = keys;
        }

        protected List<ApplicationKey> getKeys() {
            return keys;
        }

        @Override
        public ApplicationKeysConfig build() {
            return new ApplicationKeysConfig(this);
        }
    }

    ApplicationKeysConfig(final Builder config) {
        super(config);
    }
}
