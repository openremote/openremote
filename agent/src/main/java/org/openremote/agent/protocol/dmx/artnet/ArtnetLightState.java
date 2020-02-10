package org.openremote.agent.protocol.dmx.artnet;

import org.openremote.agent.protocol.dmx.AbstractDMXLightState;

public class ArtnetLightState extends AbstractDMXLightState {

    private int r;
    private int g;
    private int b;
    private int w;

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getW() {
        return w;
    }

    public double getDim() {
        return dim;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private double dim;
    private boolean enabled;

    public ArtnetLightState(int lightId, int r, int g, int b, int w, double dim, boolean enabled) {
        super(lightId);
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
        this.dim = dim;
        this.enabled = enabled;
    }

}
