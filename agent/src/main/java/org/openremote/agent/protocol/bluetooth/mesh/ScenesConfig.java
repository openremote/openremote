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
