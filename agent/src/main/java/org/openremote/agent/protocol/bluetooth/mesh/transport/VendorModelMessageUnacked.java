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
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class when creating a unacknowledged VendorModel message.
 */
public class VendorModelMessageUnacked extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(VendorModelMessageUnacked.class.getName());

    private final int mModelIdentifier;
    private final int mCompanyIdentifier;
    private final int mOpCode;

    /**
     * Constructs VendorModelMessageAcked message.
     *
     * @param appKey            {@link ApplicationKey} for this message
     * @param modelId           model identifier
     * @param companyIdentifier Company identifier of the vendor model
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public VendorModelMessageUnacked(final ApplicationKey appKey,
                                     final int modelId,
                                     final int companyIdentifier,
                                     final int mOpCode,
                                     /* @Nullable */ final byte[] parameters) {
        super(appKey);
        this.mModelIdentifier = modelId;
        this.mCompanyIdentifier = companyIdentifier;
        this.mOpCode = mOpCode;
        mParameters = parameters;
        assembleMessageParameters();
    }

    @Override
    final void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());
    }

    @Override
    public int getOpCode() {
        return mOpCode;
    }

    /**
     * Returns the company identifier of the model
     *
     * @return 16-bit company identifier
     */
    public final int getCompanyIdentifier() {
        return mCompanyIdentifier;
    }

    /**
     * Returns the model identifier for this message
     */
    public int getModelIdentifier() {
        return mModelIdentifier;
    }
}
