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

public class ScenesConfig extends ExportConfig {

    /**
     * Use this class to configure all Scenes. Exported scenes will not contain addresses of excluded nodes.
     */
    public static class ExportAll implements Builder {
        @Override
        public ScenesConfig build() {
            return new ScenesConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting the related Scenes, the scenes will not contain addresses of excluded nodes.
     */
    public static class ExportRelated implements Builder {
        @Override
        public ScenesConfig build() {
            return new ScenesConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting some of the Scenes.
     */
    public static class ExportSome implements Builder {

        private final List<Scene> scenes;

        /**
         * Constructs ExportSome to export only a selected number of Scenes when exporting a mesh network.
         * The scenes will not contain addresses of excluded nodes.
         *
         * @param scenes List of Scenes to export.
         */
        public ExportSome(final List<Scene> scenes) {
            this.scenes = scenes;
        }

        protected List<Scene> getScenes() {
            return scenes;
        }

        @Override
        public ScenesConfig build() {
            return new ScenesConfig(this);
        }
    }

    ScenesConfig(final Builder config) {
        super(config);
    }
}
