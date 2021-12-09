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
import org.openremote.agent.protocol.bluetooth.mesh.MeshManagerApi;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.container.concurrent.ContainerScheduledExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * MeshTransport class is responsible for building the configuration and application layer mesh messages.
 */
final class MeshTransport extends NetworkLayer {

    public static final Logger LOG = Logger.getLogger(MeshTransport.class.getName());

    private static final int PROXY_CONFIGURATION_TTL = 0;

    /**
     * Constructs the MeshTransport
     *
     */
    /*
    MeshTransport(@NonNull final Context context) {
        this.mContext = context;
        initHandler();
    }
     */
    MeshTransport() {
        super();
        executor = new ContainerScheduledExecutor("Bluetooth-Mesh Thread Pool", 2);
        initHandler();
    }

    /**
     * Constructs MeshTransport
     *
     * @param node    Mesh node
     */
    MeshTransport(final ProvisionedMeshNode node) {
        super();
        // this.mContext = context;
        this.mMeshNode = node;
        executor = new ContainerScheduledExecutor("Bluetooth-Mesh Thread Pool", 2);
        initHandler();
    }

    private final ScheduledThreadPoolExecutor executor;
    private final List<Future<?>> tasks = new CopyOnWriteArrayList<>();

    @Override
    protected Future<?> startTask(Runnable runnable, Long delay) {
        Future<?> task = null;
        if (delay == null || delay == 0) {
            task = executor.submit(runnable);
        } else {
            task = executor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        }
        tasks.add(task);
        return task;
    }

    @Override
    protected void stopTask(Future<?> task) {
        if (task != null) {
            task.cancel(true);
            tasks.remove(task);
        }
    }

    @Override
    protected void stopAllTasks() {
        List<Future<?>> allTasks = new ArrayList<>(tasks);
        for (Future<?> curTask : allTasks) {
            stopTask(curTask);
        }
    }

    // @Override
    protected final void initHandler() {
        // this.mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    protected synchronized final void setLowerTransportLayerCallbacks(final LowerTransportLayerCallbacks callbacks) {
        mLowerTransportLayerCallbacks = callbacks;
    }

    @Override
    final synchronized void setNetworkLayerCallbacks(final NetworkLayerCallbacks callbacks) {
        this.mNetworkLayerCallbacks = callbacks;
    }

    @Override
    final synchronized void setUpperTransportLayerCallbacks(final UpperTransportLayerCallbacks callbacks) {
        this.mUpperTransportLayerCallbacks = callbacks;
    }

    /**
     * Creates the an acknowledgement message for the received segmented messages
     *
     * @param controlMessage Control message containing the required opcodes and parameters to create the message
     * @return Control message containing the acknowledgement message pdu
     */
    final synchronized ControlMessage createSegmentBlockAcknowledgementMessage(final ControlMessage controlMessage) {
        createLowerTransportControlPDU(controlMessage);
        createNetworkLayerPDU(controlMessage);
        return controlMessage;
    }

    /**
     * Creates an access message to be sent to the peripheral node
     * <p>
     * This method will create the access message and propagate the message through the transport layers to create the final mesh pdu.
     * </p>
     *
     * @param src                     Source address of the provisioner/configurator.
     * @param dst                     Destination address to be sent to
     * @param key                     Device Key
     * @param akf                     Application key flag defines which key to be used to decrypt the message i.e device key or application key.
     * @param aid                     Identifier of the application key.
     * @param aszmic                  Defines the length of the transport mic length where 1 will encrypt withn 64 bit and 0 with 32 bit encryption.
     * @param accessOpCode            Operation code for the access message.
     * @param accessMessageParameters Parameters for the access message.
     * @return access message containing the mesh pdu
     */
    final synchronized AccessMessage createMeshMessage(final int src,
                                          final int dst,
                                          /* @Nullable */ final Integer ttl,
                                          final byte[] key,
                                          final int akf,
                                          final int aid,
                                          final int aszmic,
                                          final int accessOpCode,
                                          final byte[] accessMessageParameters) {
        final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(src);
        final int sequenceNumber = node.incrementSequenceNumber();
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);

        LOG.info("Src address: " + MeshAddress.formatAddress(src, false));
        LOG.info("Dst address: " + MeshAddress.formatAddress(dst, false));
        LOG.info("Key: " + MeshParserUtils.bytesToHex(key, false));
        LOG.info("akf: " + akf);
        LOG.info("aid: " + aid);
        LOG.info("aszmic: " + aszmic);
        LOG.info("Sequence number: " + sequenceNumber);
        LOG.info("Access message opcode: " + Integer.toHexString(accessOpCode));
        LOG.info("Access message parameters: " + MeshParserUtils.bytesToHex(accessMessageParameters, false));

        final AccessMessage message = new AccessMessage();
        message.setSrc(src);
        message.setDst(dst);
        message.setTtl(ttl == null ? node.getTtl() : ttl);
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        message.setSequenceNumber(sequenceNum);
        message.setDeviceKey(key);
        message.setAkf(akf);
        message.setAid(aid);
        message.setAszmic(aszmic);
        message.setOpCode(accessOpCode);
        message.setParameters(accessMessageParameters);
        message.setPduType(MeshManagerApi.PDU_TYPE_NETWORK);

        super.createMeshMessage(message);
        return message;
    }

    /**
     * Creates an access message to be sent to the peripheral node
     * <p>
     * This method will create the access message and propagate the message through the transport layers to create the final mesh pdu.
     * </p>
     *
     * @param src                     Source address of the provisioner/configurator.
     * @param dst                     Destination address to be sent to
     * @param label                   Label UUID for destination address
     * @param key                     Application Key
     * @param akf                     Application key flag defines which key to be used to decrypt the message i.e device key or application key.
     * @param aid                     Identifier of the application key.
     * @param aszmic                  Defines the length of the transport mic length where 1 will encrypt withn 64 bit and 0 with 32 bit encryption.
     * @param accessOpCode            Operation code for the access message.
     * @param accessMessageParameters Parameters for the access message.
     * @return access message containing the mesh pdu
     */
    final synchronized AccessMessage createMeshMessage(final int src,
                                          final int dst,
                                          /* @Nullable */ final UUID label,
                                          /* @Nullable */ final Integer ttl,
                                          /* @NonNull */ final ApplicationKey key,
                                          final int akf,
                                          final int aid,
                                          final int aszmic,
                                          final int accessOpCode,
                                          /* @Nullable */ final byte[] accessMessageParameters) {
        final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(src);
        final int sequenceNumber = node.incrementSequenceNumber();
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);

        LOG.info("Src address: " + MeshAddress.formatAddress(src, false));
        LOG.info("Dst address: " + MeshAddress.formatAddress(dst, false));
        LOG.info("Key: " + MeshParserUtils.bytesToHex(key.getKey(), false));
        LOG.info("akf: " + akf);
        LOG.info("aid: " + aid);
        LOG.info("aszmic: " + aszmic);
        LOG.info("Sequence number: " + sequenceNumber);
        LOG.info("Access message opcode: " + Integer.toHexString(accessOpCode));
        LOG.info("Access message parameters: " + MeshParserUtils.bytesToHex(accessMessageParameters, false));

        final AccessMessage message = new AccessMessage();
        message.setSrc(src);
        message.setDst(dst);
        message.setTtl(ttl == null ? node.getTtl() : ttl);
        if (label != null) {
            message.setLabel(label);
        }
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        message.setSequenceNumber(sequenceNum);
        message.setApplicationKey(key);
        message.setAkf(akf);
        message.setAid(aid);
        message.setAszmic(aszmic);
        message.setOpCode(accessOpCode);
        message.setParameters(accessMessageParameters);
        message.setPduType(MeshManagerApi.PDU_TYPE_NETWORK);

        super.createMeshMessage(message);
        return message;
    }

    /**
     * Creates a vendor model access message to be sent to the peripheral node
     * <p>
     * This method will create the access message and propagate the message through the transport layers to create the final mesh pdu.
     * </p>
     *
     * @param src                     Source address of the provisioner/configurator.
     * @param dst                     Destination address to be sent to
     * @param label                   Label UUID
     * @param key                     Application key
     * @param akf                     Application key flag defines which key to be used to decrypt the message i.e device key or application key.
     * @param aid                     Identifier of the application key.
     * @param aszmic                  Defines the length of the transport mic length where 1 will encrypt within 64 bit and 0 with 32 bit encryption.
     * @param accessOpCode            Operation code for the access message.
     * @param accessMessageParameters Parameters for the access message.
     * @return access message containing the mesh pdu
     */
    final synchronized AccessMessage createVendorMeshMessage(final int companyIdentifier,
                                                final int src,
                                                final int dst,
                                                /* @Nullable */ final UUID label,
                                                /* @Nullable */ final Integer ttl,
                                                /* @NonNull */ final ApplicationKey key,
                                                final int akf,
                                                final int aid,
                                                final int aszmic,
                                                final int accessOpCode,
                                                /* @Nullable */ final byte[] accessMessageParameters) {
        final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(src);
        final int sequenceNumber = node.incrementSequenceNumber();
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);

        LOG.info("Src address: " + MeshAddress.formatAddress(src, false));
        LOG.info("Dst address: " + MeshAddress.formatAddress(dst, false));
        LOG.info("Key: " + MeshParserUtils.bytesToHex(key.getKey(), false));
        LOG.info("akf: " + akf);
        LOG.info("aid: " + aid);
        LOG.info("aszmic: " + aszmic);
        LOG.info("Sequence number: " + sequenceNumber);
        LOG.info("Access message opcode: " + Integer.toHexString(accessOpCode));
        LOG.info("Access message parameters: " + MeshParserUtils.bytesToHex(accessMessageParameters, false));

        final AccessMessage message = new AccessMessage();
        message.setCompanyIdentifier(companyIdentifier);
        message.setSrc(src);
        message.setDst(dst);
        message.setTtl(ttl == null ? node.getTtl() : ttl);
        if (label != null) {
            message.setLabel(label);
        }
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        message.setSequenceNumber(sequenceNum);
        message.setApplicationKey(key);
        message.setAkf(akf);
        message.setAid(aid);
        message.setAszmic(aszmic);
        message.setOpCode(accessOpCode);
        message.setParameters(accessMessageParameters);
        message.setPduType(MeshManagerApi.PDU_TYPE_NETWORK);

        super.createVendorMeshMessage(message);
        return message;
    }

    /**
     * Creates a proxy configuration message to be sent to the peripheral node
     *
     * @param src        Source address of the provisioner/configurator.
     * @param dst        destination address to be sent to
     * @param opcode     Operation code for the access message.
     * @param parameters Parameters for the access message.
     * @return Control message containing the proxy configuration pdu
     */
    final synchronized ControlMessage createProxyConfigurationMessage(final int src,
                                                         final int dst,
                                                         final int opcode, final byte[] parameters) {
        final ProvisionedMeshNode node = mUpperTransportLayerCallbacks.getNode(src);
        final int sequenceNumber = node.incrementSequenceNumber();
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);

        LOG.info("Src address: " + MeshAddress.formatAddress(src, false));
        LOG.info("Dst address: " + MeshAddress.formatAddress(dst, false));
        LOG.info("Sequence number: " + sequenceNumber);
        LOG.info("Control message opcode: " + Integer.toHexString(opcode));
        LOG.info("Control message parameters: " + MeshParserUtils.bytesToHex(parameters, false));

        final ControlMessage message = new ControlMessage();
        message.setSrc(src);
        message.setDst(dst);
        message.setTtl(node.getTtl());
        message.setTtl(PROXY_CONFIGURATION_TTL); //TTL for proxy configuration messages are set to 0
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        message.setSequenceNumber(sequenceNum);
        message.setOpCode(opcode);
        message.setParameters(parameters);
        message.setPduType(MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION);

        super.createMeshMessage(message);
        return message;
    }

    final synchronized Message createRetransmitMeshMessage(final Message message, final int segment) {
        return createRetransmitNetworkLayerPDU(message, segment);
    }
}
