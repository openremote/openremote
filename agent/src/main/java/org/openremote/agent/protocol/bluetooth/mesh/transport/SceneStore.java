package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.Scene.isValidSceneNumber;

/**
 * To be used as a wrapper class when creating a SceneStore message.
 */
public class SceneStore extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(SceneStore.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.SCENE_STORE;
    private static final int SCENE_STORE_PARAMS_LENGTH = 2;

    private int mSceneNumber;

    /**
     * Constructs SceneStore message.
     *
     * @param appKey      Application key for this message
     * @param sceneNumber Scene number of SceneStore message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public SceneStore(final ApplicationKey appKey,
                      final int sceneNumber) {
        super(appKey);
        if (isValidSceneNumber(sceneNumber))
            this.mSceneNumber = sceneNumber;
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
        LOG.info("Scene Number: " + mSceneNumber);
        paramsBuffer = ByteBuffer.allocate(SCENE_STORE_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.putShort((short) mSceneNumber);
        mParameters = paramsBuffer.array();
    }
}

