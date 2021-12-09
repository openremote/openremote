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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the features supported by a {@link ProvisionedMeshNode}
 */
public class Features {

    // @IntDef({DISABLED, ENABLED, UNSUPPORTED})
    // @interface FeatureState {
    // }

    // Key refresh phases
    public static final int DISABLED = 0; //Feature is disabled
    public static final int ENABLED = 1; //Feature is enabled
    public static final int UNSUPPORTED = 2; //Feature is not supported

    private int friend; //friend feature
    private int lowPower; //low power feature
    private int proxy; //proxy feature
    private int relay; //relay feature

    //
    // Constructs the features of a provisioned node
    //
    // @param friend   Specifies if the friend feature is supported based on {@link FeatureState}
    // @param lowPower Specifies if the low power feature is supported based on {@link FeatureState}
    // @param proxy    Specifies if the proxy feature is supported based on {@link FeatureState}
    // @param relay    Specifies if the relay feature is supported based on {@link FeatureState}
    //
    public Features(/*@FeatureState*/ final int friend, /*@FeatureState*/ final int lowPower, /*@FeatureState*/ final int proxy, /*@FeatureState*/ final int relay) {
        this.friend = friend;
        this.lowPower = lowPower;
        this.proxy = proxy;
        this.relay = relay;
    }

    @Override
    public String toString() {
        return "Features{" +
            "friend=" + friend +
            ", lowPower=" + lowPower +
            ", proxy=" + proxy +
            ", relay=" + relay +
            '}';
    }

    /**
     * Returns the friend feature state
     */
    // @FeatureState
    public int getFriend() {
        return friend;
    }

    //
    // Sets the friend feature of the node
    //
    // @param friend {@link FeatureState}
    //
    public void setFriend(/*@FeatureState*/ final int friend) {
        this.friend = friend;
    }

    /**
     * Returns the low power feature state
     */
    // @FeatureState
    public int getLowPower() {
        return lowPower;
    }

    //
    // Sets the low power feature of the node
    //
    // @param lowPower {@link FeatureState}
    //
    public void setLowPower(/*@FeatureState*/ final int lowPower) {
        this.lowPower = lowPower;
    }

    /**
     * Returns the proxy feature state
     */
    // @FeatureState
    public int getProxy() {
        return proxy;
    }

    //
    // Sets the proxy feature of the node
    //
    // @param proxy {@link FeatureState}
    //
    public void setProxy(/* @FeatureState */ final int proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns the relay feature state
     */
    // @FeatureState
    public int getRelay() {
        return relay;
    }

    //
    // Sets the relay feature of the node
    //
    // @param relay {@link FeatureState}
    //
    public void setRelay(/*@FeatureState*/ final int relay) {
        this.relay = relay;
    }

    /**
     * Returns true if friend feature is supported and false otherwise
     */
    public boolean isFriendFeatureSupported() {
        switch (friend) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if relay feature is supported and false otherwise
     */
    public boolean isRelayFeatureSupported() {
        switch (relay) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if proxy feature is supported and false otherwise
     */
    public boolean isProxyFeatureSupported() {
        switch (proxy) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if low power feature is supported and false otherwise
     */
    public boolean isLowPowerFeatureSupported() {
        switch (lowPower) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    public int assembleFeatures() {
        int features = bitValue(lowPower) << 3;
        features = features | bitValue(friend) << 2;
        features = features | bitValue(proxy) << 1;
        features = features | bitValue(relay);
        return features;
    }

    private short bitValue(final int feature) {
        switch (feature) {
            default:
            case UNSUPPORTED:
            case DISABLED:
                return 0;
            case ENABLED:
                return 1;
        }
    }
}
