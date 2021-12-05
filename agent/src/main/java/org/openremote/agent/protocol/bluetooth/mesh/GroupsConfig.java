package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.List;

/**
 * Contains the configuration required when exporting a selected number of Groups in a mesh network.
 */
public class GroupsConfig extends ExportConfig {

    /**
     * Use this class to configure when exporting all the Groups.
     */
    public static class ExportAll implements Builder {
        @Override
        public GroupsConfig build() {
            return new GroupsConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting the related Groups, which means that the exported
     * configuration will only contain those groups that any exported model is subscribed or publishing to
     */
    public static class ExportRelated implements Builder {
        @Override
        public GroupsConfig build() {
            return new GroupsConfig(this);
        }
    }

    /**
     * Use this class to configure when exporting some of the groups.
     */
    public static class ExportSome implements Builder {

        private final List<Group> groups;

        /**
         * Constructs ExportSome to export only a selected number of Groups when exporting a mesh network.
         * Excluded groups will also be excluded from subscription lists and publish information in exported Models.
         *
         * @param groups List of Groups to export.
         */
        public ExportSome(final List<Group> groups) {
            this.groups = groups;
        }

        protected List<Group> getGroups() {
            return groups;
        }

        @Override
        public ExportConfig build() {
            return new GroupsConfig(this);
        }
    }

    GroupsConfig(final Builder config) {
        super(config);
    }
}

