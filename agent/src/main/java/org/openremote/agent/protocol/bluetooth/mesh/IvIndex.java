package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.Calendar;

/**
 * Class containing the current IV Index State of the network.
 */
public class IvIndex {

    private final int ivIndex;
    private final boolean isIvUpdateActive; // False: Normal Operation, True: IV Update in progress
    private boolean ivRecoveryFlag = false;
    private Calendar transitionDate;

    /**
     * Construct the IV Index state of the mesh network
     *
     * @param ivIndex          IV Index of the network.
     * @param isIvUpdateActive If true IV Update is in progress and false the network is in Normal operation.
     * @param transitionDate   Time when the last IV Update happened
     */
    public IvIndex(final int ivIndex, final boolean isIvUpdateActive, final Calendar transitionDate) {
        this.ivIndex = ivIndex;
        this.isIvUpdateActive = isIvUpdateActive;
        this.transitionDate = transitionDate;
    }

    @Override
    public String toString() {
        return "IV Index: " + ivIndex + ", IV Update Active: " + isIvUpdateActive;
    }

    /**
     * Returns current iv index
     */
    public int getIvIndex() {
        return ivIndex;
    }

    /**
     * Returns the current iv update flag.
     */
    public boolean isIvUpdateActive() {
        return isIvUpdateActive;
    }

    /**
     * Returns iv index used when transmitting messages.
     */
    public int getTransmitIvIndex() {
        return (isIvUpdateActive && ivIndex != 0) ? ivIndex - 1 : ivIndex;
    }

    public boolean getIvRecoveryFlag() {
        return ivRecoveryFlag;
    }

    public void setIvRecoveryFlag(boolean ivRecoveryFlag) {
        this.ivRecoveryFlag = ivRecoveryFlag;
    }

    public Calendar getTransitionDate() {
        return transitionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IvIndex ivIndex1 = (IvIndex) o;
        return ivIndex == ivIndex1.ivIndex &&
            isIvUpdateActive == ivIndex1.isIvUpdateActive &&
            ivRecoveryFlag == ivIndex1.ivRecoveryFlag &&
            (transitionDate.getTimeInMillis() == ivIndex1.transitionDate.getTimeInMillis());
    }
}
