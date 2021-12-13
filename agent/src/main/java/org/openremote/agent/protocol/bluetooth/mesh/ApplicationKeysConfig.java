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
