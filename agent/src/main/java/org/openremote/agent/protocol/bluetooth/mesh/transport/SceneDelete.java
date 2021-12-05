package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.Scene.isValidSceneNumber;

/**
 * To be used as a wrapper class when creating a SceneDelete message.
 */
public class SceneDelete extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(SceneDelete.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.SCENE_DELETE;
    private static final int SCENE_DELETE_PARAMS_LENGTH = 2;

    private int sceneNumber;

    /**
     * Constructs SceneDelete message.
     *
     * @param appKey      {@link ApplicationKey} key for this message
     * @param sceneNumber Scene number of SceneDelete message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public SceneDelete(final ApplicationKey appKey,
                       final int sceneNumber) {
        super(appKey);
        if (isValidSceneNumber(sceneNumber))
            this.sceneNumber = sceneNumber;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());
        final ByteBuffer paramsBuffer;
        LOG.info("Scene Number: " + sceneNumber);
        paramsBuffer = ByteBuffer.allocate(SCENE_DELETE_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.putShort((short) sceneNumber);
        mParameters = paramsBuffer.array();
    }

    public int getSceneNumber() {
        return sceneNumber;
    }
}

