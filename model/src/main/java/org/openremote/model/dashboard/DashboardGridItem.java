package org.openremote.model.dashboard;

import jakarta.validation.constraints.Min;

public class DashboardGridItem {

    // Fields
    protected String id;
    protected int x;
    protected int y;

    @Min(value = 1, message = "{Dashboard.gridItem.w.Min}")
    protected int w;

    @Min(value = 1, message = "{Dashboard.gridItem.h.Min}")
    protected int h;

    protected int minH;
    protected int minW;
    protected int minPixelH;
    protected int minPixelW;
    protected boolean noResize;
    protected boolean noMove;
    protected boolean locked;

    public DashboardGridItem setX(int x) {
        this.x = x;
        return this;
    }
    public DashboardGridItem setY(int y) {
        this.y = y;
        return this;
    }
    public DashboardGridItem setW(int w) {
        this.w = w;
        return this;
    }
    public DashboardGridItem setH(int h) {
        this.h = h;
        return this;
    }
}
