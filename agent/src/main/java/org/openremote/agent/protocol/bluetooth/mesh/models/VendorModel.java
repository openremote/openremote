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
package org.openremote.agent.protocol.bluetooth.mesh.models;

import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshModel;
import org.openremote.agent.protocol.bluetooth.mesh.utils.CompanyIdentifiers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class VendorModel extends MeshModel {

    public static final Logger LOG = Logger.getLogger(VendorModel.class.getName());

    private final short companyIdentifier;
    private final String companyName;

    public VendorModel(final int modelIdentifier) {
        super(modelIdentifier);
        final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(modelIdentifier);
        this.companyIdentifier = buffer.getShort(0);
        this.companyName = CompanyIdentifiers.getCompanyName(companyIdentifier);
        LOG.info("Company name: " + companyName);
    }

    @Override
    public int getModelId() {
        return mModelId;
    }

    @Override
    public String getModelName() {
        return "Vendor Model";
    }

    public int getCompanyIdentifier() {
        return companyIdentifier;
    }

    public String getCompanyName() {
        return companyName;
    }
}
