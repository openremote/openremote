package org.openremote.agent.protocol.bluetooth.mesh;

public class NodeKey {
    private final int index;
    private boolean updated;

    /**
     * Constructs a NodeKey
     *
     * @param index Index of the key
     */
    public NodeKey(final int index) {
        this(index, false);
    }

    /**
     * Constructs a NodeKey
     *
     * @param index   Index of the key
     * @param updated If the key has been updated
     */
    public NodeKey(final int index, final boolean updated) {
        this.index = index;
        this.updated = updated;
    }

    /**
     * Returns the index of the added key
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns true if the key has been updated
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Sets the updated state of the network/application key
     *
     * @param updated true if updated and false otherwise
     */
    public void setUpdated(final boolean updated) {
        this.updated = updated;
    }
}