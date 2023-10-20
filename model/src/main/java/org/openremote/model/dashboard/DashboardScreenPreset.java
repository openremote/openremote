package org.openremote.model.dashboard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class DashboardScreenPreset {

    // Fields
    protected String id;

    @NotBlank(message = "{Dashboard.screenPreset.displayName.NotBlank}")
    protected String displayName;

    @Min(value = 1, message = "{Dashboard.screenPreset.breakpoint.Min}")
    protected int breakpoint;

    @NotNull(message = "{Dashboard.screenPreset.scalingPreset.NotNull}")
    protected DashboardScalingPreset scalingPreset;

    protected String redirectDashboardId; // nullable


    /* -------------------------------- */

    public DashboardScreenPreset() {
    }
    public DashboardScreenPreset(String displayName, DashboardScalingPreset scalingPreset) {
        this.displayName = displayName;
        this.breakpoint = 1;
        this.scalingPreset = scalingPreset;
    }

    public void setId(@NotNull @NotEmpty String id) { this.id = id; }
    public void setDisplayName(@NotNull @NotEmpty String displayName) { this.displayName = displayName; }
    public void setBreakpoint(@NotNull int breakpoint) { this.breakpoint = breakpoint; }
    public void setScalingPreset(@NotNull DashboardScalingPreset scalingPreset) { this.scalingPreset = scalingPreset; }
    public void setRedirectDashboardId(String redirectDashboardId) {
        if((redirectDashboardId == null || redirectDashboardId.isEmpty()) && this.scalingPreset == DashboardScalingPreset.REDIRECT) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        this.redirectDashboardId = redirectDashboardId;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getBreakpoint() { return breakpoint; }
    public DashboardScalingPreset getScalingPreset() { return scalingPreset; }
    public String getRedirectDashboardId() { return redirectDashboardId; }
}
