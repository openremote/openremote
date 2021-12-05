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
