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
package org.openremote.manager.shared.device;

/**
 * Describes the capabilities that an agent connectors's inventory
 * offers; inventory read is mandatory. This is how device provisioning
 * should be achieved.
 */
public class InventoryCapabilities {
    protected boolean inventoryCreate;
    protected boolean inventoryUpdate;
    protected boolean inventoryDelete;

    public InventoryCapabilities(boolean inventoryCreate, boolean inventoryUpdate, boolean inventoryDelete) {
        this.inventoryCreate = inventoryCreate;
        this.inventoryUpdate = inventoryUpdate;
        this.inventoryDelete = inventoryDelete;
    }

    public boolean isInventoryCreate() {
        return inventoryCreate;
    }

    public boolean isInventoryUpdate() {
        return inventoryUpdate;
    }

    public boolean isInventoryDelete() {
        return inventoryDelete;
    }
}
