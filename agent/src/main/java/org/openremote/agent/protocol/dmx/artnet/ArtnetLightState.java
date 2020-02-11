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

    public void setR(int r) {
        this.r = r;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setDim(double dim) {
        this.dim = dim;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    @Override
    public Byte[] getValues() {
        return new Byte[]{(byte)this.getG(),(byte)this.getR(),(byte)this.getB(),(byte)this.getW()};
    }
}
