/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.assets;

import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.shared.Consumer;

import java.util.List;

public interface AssetsView extends IsWidget {

    interface Presenter {
        void loadAssetChildren(Asset parent, Consumer<List<Asset>> consumer);
        void onAssetSelected(Asset asset);
    }

    class Asset {
        public String id;
        public String type;
        public String displayName;
        public String location;

        public Asset() {
        }

        public Asset(String id, String type, String displayName, String location) {
            this.id = id;
            this.type = type;
            this.displayName = displayName;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    void setPresenter(Presenter presenter);

    void setAssetDisplayName(String name);
}
