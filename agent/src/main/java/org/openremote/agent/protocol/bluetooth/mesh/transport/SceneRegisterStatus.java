package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the GenericOnOffStatus Message.
 */
public final class SceneRegisterStatus extends GenericStatusMessage implements SceneStatuses {
    private static final int SCENE_REGISTER_STATUS_MANDATORY_LENGTH = 3;
    public static final Logger LOG = Logger.getLogger(SceneRegisterStatus.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.SCENE_REGISTER_STATUS;
    private int mStatus;
    private int mCurrentScene;
    private final ArrayList<Integer> mSceneList = new ArrayList<>();

    /**
     * Constructs the GenericOnOffStatus mMessage.
     *
     * @param message Access Message
     */
    public SceneRegisterStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received scene register status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        mStatus = buffer.get() & 0xFF;
        mCurrentScene = buffer.getShort() & 0xFFFF;
        LOG.info("Status: " + mStatus);
        LOG.info("Current Scene: " + mCurrentScene);
        if (buffer.limit() > SCENE_REGISTER_STATUS_MANDATORY_LENGTH) {
            int sceneCount = (buffer.limit() - SCENE_REGISTER_STATUS_MANDATORY_LENGTH) / 2;
            for (int i = 0; i < sceneCount; i++) {
                mSceneList.add(buffer.getShort() & 0xFFFF);
            }
            LOG.info("Scenes stored: " + sceneCount);
        }
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the present state of the GenericOnOffModel
     *
     * @return true if on and false other wise
     */
    public final int getStatus() {
        return mStatus;
    }

    public boolean isSuccessful() {
        return mStatus == 0x00;
    }

    /**
     * Returns the target state of the GenericOnOffModel
     *
     * @return true if on and false other wise
     */
    public final int getCurrentScene() {
        return mCurrentScene;
    }

    /**
     * Returns the scene list.
     *
     * @return scene list
     */
    public ArrayList<Integer> getSceneList() {
        return mSceneList;
    }
}

