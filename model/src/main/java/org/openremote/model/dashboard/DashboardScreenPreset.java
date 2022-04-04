package org.openremote.model.dashboard;

public class DashboardScreenPreset {

    // Fields
    protected String id;
    protected String displayName;
    protected int breakpoint;
    protected DashboardScalingPreset scalingPreset;
    protected String redirectDashboardId; // nullable

    public DashboardScreenPreset() {

    }

    public boolean checkValidity() {
        if(id != null && !id.isEmpty() && displayName != null && !displayName.isEmpty() && breakpoint > 0 && scalingPreset != null && !scalingPreset.toString().isEmpty()) {
            // TODO: more validity checks such as whether the ID on redirectDashboardId exists
            return true;
        }
        return false;
    }
}
