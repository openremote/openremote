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

import org.openremote.agent.protocol.bluetooth.mesh.models.SigModelParser;
import org.openremote.agent.protocol.bluetooth.mesh.models.VendorModel;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.DeviceFeatureUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigCompositionDataStatus Message.
 */
public class ConfigCompositionDataStatus extends ConfigStatusMessage{

    public static final Logger LOG = Logger.getLogger(ConfigCompositionDataStatus.class.getName());

    private static final String TAG = ConfigCompositionDataStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_COMPOSITION_DATA_STATUS;
    private static final int ELEMENTS_OFFSET = 12;
    private int companyIdentifier;
    private int productIdentifier;
    private int versionIdentifier;
    private int crpl;
    private int features;
    private boolean relayFeatureSupported;
    private boolean proxyFeatureSupported;
    private boolean friendFeatureSupported;
    private boolean lowPowerFeatureSupported;
    private Map<Integer, Element> mElements = new LinkedHashMap<>();

    /**
     * Constructs the ConfigCompositionDataStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigCompositionDataStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        parseCompositionDataPages();
    }

    /**
     * Parses composition data status received from the mesh node
     */
    private void parseCompositionDataPages() {
        final AccessMessage message = (AccessMessage) mMessage;
        final byte[] accessPayload = message.getAccessPdu();

        //Bluetooth SIG 16-bit company identifier
        companyIdentifier = MeshParserUtils.unsignedBytesToInt(accessPayload[2], accessPayload[3]);
        LOG.info("Company identifier: " + String.format(Locale.US, "%04X", companyIdentifier));

        //16-bit vendor-assigned product identifier;
        productIdentifier = MeshParserUtils.unsignedBytesToInt(accessPayload[4], accessPayload[5]);
        LOG.info("Product identifier: " + String.format(Locale.US, "%04X", productIdentifier));

        //16-bit vendor-assigned product version identifier;
        versionIdentifier = MeshParserUtils.unsignedBytesToInt(accessPayload[6], accessPayload[7]);
        LOG.info("Version identifier: " + String.format(Locale.US, "%04X", versionIdentifier));

        //16-bit representation of the minimum number of replay protection list entries in a device
        crpl = MeshParserUtils.unsignedBytesToInt(accessPayload[8], accessPayload[9]);
        LOG.info("crpl: " + String.format(Locale.US, "%04X", crpl));

        //16-bit device features
        features = MeshParserUtils.unsignedBytesToInt(accessPayload[10], accessPayload[11]);
        LOG.info("Features: " + String.format(Locale.US, "%04X", features));

        relayFeatureSupported = DeviceFeatureUtils.supportsRelayFeature(features);
        LOG.info("Relay feature: " + relayFeatureSupported);

        proxyFeatureSupported = DeviceFeatureUtils.supportsProxyFeature(features);
        LOG.info("Proxy feature: " + proxyFeatureSupported);

        friendFeatureSupported = DeviceFeatureUtils.supportsFriendFeature(features);
        LOG.info("Friend feature: " + friendFeatureSupported);

        lowPowerFeatureSupported = DeviceFeatureUtils.supportsLowPowerFeature(features);
        LOG.info("Low power feature: " + lowPowerFeatureSupported);

        // Parsing the elements which is a variable number of octets
        // Elements contain following
        // location descriptor,
        // Number of SIG model IDs in this element
        // Number of vendor model in this element
        // SIG model ID octents - Variable
        // Vendor model ID octents - Variable
        parseElements(accessPayload, message.getSrc());
        LOG.info("Number of elements: " + mElements.size());
    }

    /**
     * Parses the elements within the composition data status
     *
     * @param accessPayload underlying payload containing the elements
     * @param src           source address
     */
    private void parseElements(final byte[] accessPayload, final int src) {
        int tempOffset = ELEMENTS_OFFSET;
        int counter = 0;
        int elementAddress = 0;
        while (tempOffset < accessPayload.length) {
            final Map<Integer, MeshModel> models = new LinkedHashMap<>();
            final int locationDescriptor = accessPayload[tempOffset + 1] << 8 | accessPayload[tempOffset];
            LOG.info("Location identifier: " + String.format(Locale.US, "%04X", locationDescriptor));

            tempOffset = tempOffset + 2;
            final int numSigModelIds = accessPayload[tempOffset];
            LOG.info("Number of sig models: " + String.format(Locale.US, "%04X", numSigModelIds));

            tempOffset = tempOffset + 1;
            final int numVendorModelIds = accessPayload[tempOffset];
            LOG.info("Number of vendor models: " + String.format(Locale.US, "%04X", numVendorModelIds));

            tempOffset = tempOffset + 1;
            if (numSigModelIds > 0) {
                for (int i = 0; i < numSigModelIds; i++) {
                    final int modelId = MeshParserUtils.unsignedBytesToInt(accessPayload[tempOffset], accessPayload[tempOffset + 1]);
                    models.put(modelId, SigModelParser.getSigModel(modelId)); // sig models are 16-bit
                    LOG.info("Sig model ID " + i + " : " + String.format(Locale.US, "%04X", modelId));
                    tempOffset = tempOffset + 2;
                }
            }

            if (numVendorModelIds > 0) {
                for (int i = 0; i < numVendorModelIds; i++) {
                    // vendor models are 32-bit that contains a 16-bit company identifier and a 16-bit model identifier
                    final int companyIdentifier = MeshParserUtils.unsignedBytesToInt(accessPayload[tempOffset], accessPayload[tempOffset + 1]);
                    final int modelIdentifier = MeshParserUtils.unsignedBytesToInt(accessPayload[tempOffset + 2], accessPayload[tempOffset + 3]);
                    final int vendorModelIdentifier = companyIdentifier << 16 | modelIdentifier;
                    models.put(vendorModelIdentifier, new VendorModel(vendorModelIdentifier));
                    LOG.info("Vendor - model ID " + i + " : " + String.format(Locale.US, "%08X", vendorModelIdentifier));
                    tempOffset = tempOffset + 4;
                }
            }

            if (counter == 0) {
                elementAddress = src;
            } else {
                elementAddress++;
            }
            counter++;
            final Element element = new Element(elementAddress, locationDescriptor, models);
            final int unicastAddress = elementAddress;
            mElements.put(unicastAddress, element);
        }
    }

    /**
     * Returns the 16-bit company identifier assigned by Bluetooth SIG.
     *
     * @return company identifier
     */
    public int getCompanyIdentifier() {
        return companyIdentifier;
    }

    /**
     * Returns the 16-bit vendor assigned assigned product identifier.
     *
     * @return product identifier
     */
    public int getProductIdentifier() {
        return productIdentifier;
    }

    /**
     * Returns the 16-bit vendor assigned product version identifier.
     *
     * @return version identifier
     */
    public int getVersionIdentifier() {
        return versionIdentifier;
    }

    /**
     * Returns a 16-bit value representing the minimum number of replay protection list entries in a device.
     *
     * @return crpl
     */
    public int getCrpl() {
        return crpl;
    }

    /**
     * Returns a 16-bit features field indicating the device features.
     *
     * @return features field
     */
    public int getFeatures() {
        return features;
    }

    /**
     * Returns if the relay feature is supported.
     *
     * @return true if relay features is supported or false otherwise
     */
    public boolean isRelayFeatureSupported() {
        return relayFeatureSupported;
    }

    /**
     * Returns if the proxy feature is supported.
     *
     * @return true if proxy feature is supported or false otherwise
     */
    public boolean isProxyFeatureSupported() {
        return proxyFeatureSupported;
    }

    /**
     * Returns if the friend feature is supported.
     *
     * @return true if friend feature is supported or false otherwise
     */
    public boolean isFriendFeatureSupported() {
        return friendFeatureSupported;
    }

    /**
     * Returns if the low power feature is supported.
     *
     * @return true if low power feature is supported or false otherwise
     */
    public boolean isLowPowerFeatureSupported() {
        return lowPowerFeatureSupported;
    }

    /**
     * Returns the number of elements existing in this node.
     *
     * @return number of elements
     */
    public Map<Integer, Element> getElements() {
        return mElements;
    }

    private int parseCompanyIdentifier(final short companyIdentifier) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(companyIdentifier).getShort(0);
    }

    private int parseProductIdentifier(final short productIdentifier) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(productIdentifier).getShort(0);
    }

    private int parseVersionIdentifier(final short versionIdentifier) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(versionIdentifier).getShort(0);
    }

    private int parseCrpl(final short companyIdentifier) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(companyIdentifier).getShort(0);
    }

    private int parseFeatures(final short companyIdentifier) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(companyIdentifier).getShort(0);
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

}
