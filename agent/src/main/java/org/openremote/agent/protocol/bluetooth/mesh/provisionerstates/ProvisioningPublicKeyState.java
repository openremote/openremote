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
package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

import org.openremote.agent.protocol.bluetooth.mesh.InternalTransportCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.MeshProvisioningStatusCallbacks;


import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;

public class ProvisioningPublicKeyState extends ProvisioningState {

    private static final int PROVISIONING_PUBLIC_KEY_XY_PDU_LENGTH = 69;
    public static final Logger LOG = Logger.getLogger(ProvisioningPublicKeyState.class.getName());
    private final byte[] publicKeyXY = new byte[PROVISIONING_PUBLIC_KEY_XY_PDU_LENGTH];
    private final MeshProvisioningStatusCallbacks mStatusCallbacks;
    private final UnprovisionedMeshNode mUnprovisionedMeshNode;
    private final InternalTransportCallbacks mInternalTransportCallbacks;

    private byte[] mTempProvisioneeXY;
    private int segmentCount = 0;
    private ECPrivateKey mProvisionerPrivaetKey;


    public ProvisioningPublicKeyState(final UnprovisionedMeshNode unprovisionedMeshNode, final InternalTransportCallbacks mInternalTransportCallbacks, final MeshProvisioningStatusCallbacks meshProvisioningStatusCallbacks) {
        super();
        this.mUnprovisionedMeshNode = unprovisionedMeshNode;
        this.mStatusCallbacks = meshProvisioningStatusCallbacks;
        this.mInternalTransportCallbacks = mInternalTransportCallbacks;
    }

    @Override
    public State getState() {
        return State.PROVISIONING_PUBLIC_KEY;
    }

    @Override
    public void executeSend() {
        generateKeyPairs();
        final byte[] pdu = generatePublicKeyXYPDU();
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_PUBLIC_KEY_SENT, pdu);
        mInternalTransportCallbacks.sendProvisioningPdu(mUnprovisionedMeshNode, pdu);
    }

    @Override
    public boolean parseData(final byte[] data) {
        mStatusCallbacks.onProvisioningStateChanged(mUnprovisionedMeshNode, States.PROVISIONING_PUBLIC_KEY_RECEIVED, data);
        generateSharedECDHSecret(data);
        return true;
    }

    private void generateKeyPairs() {

        try {
            final ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "SC");
            keyPairGenerator.initialize(parameterSpec);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

            mProvisionerPrivaetKey = (ECPrivateKey) keyPair.getPrivate();

            final ECPoint point = publicKey.getQ();

            final BigInteger x = point.getXCoord().toBigInteger();
            final BigInteger y = point.getYCoord().toBigInteger();
            final byte[] tempX = BigIntegers.asUnsignedByteArray(32, x);
            final byte[] tempY = BigIntegers.asUnsignedByteArray(32, y);

            LOG.info("X: length: " + tempX.length + " " + MeshParserUtils.bytesToHex(tempX, false));
            LOG.info("Y: length: " + tempY.length + " " + MeshParserUtils.bytesToHex(tempY, false));

            final byte[] tempXY = new byte[64];
            System.arraycopy(tempX, 0, tempXY, 0, tempX.length);
            System.arraycopy(tempY, 0, tempXY, tempY.length, tempY.length);

            mUnprovisionedMeshNode.setProvisionerPublicKeyXY(tempXY);

            LOG.info("XY: " + MeshParserUtils.bytesToHex(tempXY, true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] generatePublicKeyXYPDU() {

        final byte[] tempXY = mUnprovisionedMeshNode.getProvisionerPublicKeyXY();

        ByteBuffer buffer = ByteBuffer.allocate(tempXY.length + 2);
        buffer.put(MeshManagerApi.PDU_TYPE_PROVISIONING);
        buffer.put(TYPE_PROVISIONING_PUBLIC_KEY);
        buffer.put(tempXY);

        return buffer.array();
    }

    private void generateSharedECDHSecret(final byte[] provisioneePublicKeyXYPDU) {
        if (provisioneePublicKeyXYPDU.length != 66) {
            throw new IllegalArgumentException("Invalid Provisionee Public Key PDU," +
                " length of the Provisionee public key must be 66 bytes, but was " + provisioneePublicKeyXYPDU.length);
        }
        final ByteBuffer buffer = ByteBuffer.allocate(provisioneePublicKeyXYPDU.length - 2);
        buffer.put(provisioneePublicKeyXYPDU, 2, buffer.limit());
        final byte[] xy = mTempProvisioneeXY = buffer.array();
        mUnprovisionedMeshNode.setProvisioneePublicKeyXY(xy);

        final byte[] xComponent = new byte[32];
        System.arraycopy(xy, 0, xComponent, 0, xComponent.length);

        final byte[] yComponent = new byte[32];
        System.arraycopy(xy, 32, yComponent, 0, xComponent.length);

        final byte[] provisioneeX = convertToLittleEndian(xComponent, ByteOrder.LITTLE_ENDIAN);
        LOG.info("Provsionee X: " + MeshParserUtils.bytesToHex(provisioneeX, false));

        final byte[] provisioneeY = convertToLittleEndian(yComponent, ByteOrder.LITTLE_ENDIAN);
        LOG.info("Provsionee Y: " + MeshParserUtils.bytesToHex(provisioneeY, false));

        final BigInteger x = BigIntegers.fromUnsignedByteArray(xy, 0, 32);
        final BigInteger y = BigIntegers.fromUnsignedByteArray(xy, 32, 32);

        final ECParameterSpec ecParameters = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECCurve curve = ecParameters.getCurve();
        ECPoint ecPoint = curve.validatePoint(x, y);


        ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameters);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("ECDH", "SC");
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);

            KeyAgreement a = KeyAgreement.getInstance("ECDH", "SC");
            a.init(mProvisionerPrivaetKey);
            a.doPhase(publicKey, true);

            final byte[] sharedECDHSecret = a.generateSecret();
            mUnprovisionedMeshNode.setSharedECDHSecret(sharedECDHSecret);
            LOG.info("ECDH Secret: " + MeshParserUtils.bytesToHex(sharedECDHSecret, false));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private byte[] convertToLittleEndian(final byte[] data, final ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.order(order);
        buffer.put(data);
        return buffer.array();
    }
}

