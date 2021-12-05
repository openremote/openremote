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

import org.openremote.model.asset.agent.AgentLink;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Optional;

public class BluetoothMeshAgentLink extends AgentLink<BluetoothMeshAgentLink> {

    @Min(0)
    @Max(2147483647)
    @NotNull
    protected Integer appKeyIndex;

    @Pattern(regexp = "^([0-9A-Fa-f]{4})$")
    protected String address;

    @NotBlank
    protected String modelName;

    // For Hydrators
    protected BluetoothMeshAgentLink() {
    }

    public BluetoothMeshAgentLink(String id, Integer appKeyIndex, String address, String modelName) {
        super(id);
        this.appKeyIndex = appKeyIndex;
        this.address = address;
        this.modelName = modelName;
    }

    public Optional<Integer> getAppKeyIndex() {
        return Optional.ofNullable(appKeyIndex);
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<String> getModelName() {
        return Optional.ofNullable(modelName);
    }
}
