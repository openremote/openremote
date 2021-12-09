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
 * Contains the configuration required when exporting a selected number of Provisioners in a mesh network.
 */
public class ProvisionersConfig extends ExportConfig {

    /**
     * Use this class to configure when exporting all the Provisioners.
     */
    public static class ExportAll implements Builder {
        @Override
        public ProvisionersConfig build() {
            return new ProvisionersConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting some of the Provisioners.
     */
    public static class ExportSome implements Builder {

        private final List<Provisioner> provisioners;

        /**
         * Constructs ExportSome to export only a selected number of Provisioners when exporting a mesh network.
         *
         * @param provisioners List of Provisioners to export.
         * @throws IllegalArgumentException if the list does not contain at least one provisioner.
         */
        public ExportSome(final List<Provisioner> provisioners) {
            if (provisioners.isEmpty())
                throw new IllegalArgumentException("Error, at least one Provisioner must be selected!");
            this.provisioners = provisioners;
        }

        protected List<Provisioner> getProvisioners() {
            return provisioners;
        }

        @Override
        public ProvisionersConfig build() {
            return new ProvisionersConfig(this);
        }
    }

    ProvisionersConfig(final Builder config) {
        super(config);
    }
}
