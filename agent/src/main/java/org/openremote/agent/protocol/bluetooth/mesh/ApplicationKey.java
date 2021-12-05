package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import javax.validation.constraints.NotNull;

/**
 * Wrapper class for application key
 */
public final class ApplicationKey extends MeshKey {

    private int boundNetKeyIndex = 0;
    private int aid;
    private int oldAid;

    /**
     * Constructs a ApplicationKey object with a given key index and network key
     *
     * @param keyIndex 12-bit app key index
     * @param key      16-byte app key
     */
    public ApplicationKey(final int keyIndex, @NotNull final byte[] key) {
        super(keyIndex, key);
        name = "Application Key " + (keyIndex + 1);
        aid = SecureUtils.calculateK4(key);
    }

    /**
     * Returns the index of the associated netkey
     *
     * @return network key index
     */
    public int getBoundNetKeyIndex() {
        return boundNetKeyIndex;
    }

    /**
     * Set the net key index to which the app key is associated with
     *
     * @param boundNetKeyIndex network key index
     */
    public void setBoundNetKeyIndex(final int boundNetKeyIndex) {
        this.boundNetKeyIndex = boundNetKeyIndex;
    }

    @Override
    public void setKey(@NotNull final byte[] key) {
        super.setKey(key);
        aid = SecureUtils.calculateK4(key);
    }

    @Override
    public void setOldKey(final byte[] oldKey) {
        super.setOldKey(oldKey);
        if (oldKey != null)
            oldAid = SecureUtils.calculateK4(oldKey);
    }

    public int getAid() {
        return aid;
    }

    public int getOldAid() {
        return oldAid;
    }

    public ApplicationKey clone() throws CloneNotSupportedException {
        return (ApplicationKey) super.clone();
    }
}
