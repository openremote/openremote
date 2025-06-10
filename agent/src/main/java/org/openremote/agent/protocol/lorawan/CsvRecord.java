/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class CsvRecord {

    @JsonProperty("devEUI")
    private String devEUI;

    @JsonProperty("name")
    private String name;

    @JsonProperty("vendor_id")
    private String vendorId;

    @JsonProperty("model_id")
    private String modelId;

    @JsonProperty("firmwareVersion")
    private String firmwareVersion;

    public String getDevEUI() { return devEUI; }
    public void setDevEUI(String devEUI) { this.devEUI = devEUI; }

    public String getName() {
        if (name == null || name.trim().isEmpty()) {
            return Optional.of(modelId).orElse("") + "-" + Optional.of(devEUI).orElse("");
        }
        return name;
    }
    public void setName(String name) { this.name = name; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    @Override
    public String toString() {
        return "CsvRecord{" +
            "devEUI='" + (devEUI != null ? devEUI : "") + '\'' +
            ", name='" + (name != null ? name : "") + '\'' +
            ", vendorId='" + (vendorId != null ? vendorId : "") + '\'' +
            ", modelId='" + (modelId != null ? modelId : "") + '\'' +
            ", firmwareVersion='" + (firmwareVersion != null ? firmwareVersion : "") + '\'' +
            '}';
    }

    public boolean isValid() {
        return devEUI != null && !devEUI.trim().isEmpty() &&
               vendorId != null && !vendorId.trim().isEmpty() &&
               modelId != null && !modelId.trim().isEmpty();
    }
}
