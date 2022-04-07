package org.openremote.model.dashboard;

import java.util.Arrays;

public class DashboardTemplate {

    // Fields
    protected String id;
    protected int columns;
    protected int maxScreenWidth;
    protected DashboardScreenPreset[] screenPresets;
    protected DashboardWidget[] widgets;

    // Final Properties
    private static final int MAX_SCREEN_WIDTH_DEFAULT = 1080;


    // Constructor
    public DashboardTemplate() {
    }

    // TODO: still not proud of the validity check here, probably needs to move
    public boolean checkValidity() {
        if(!(maxScreenWidth > 0)) { maxScreenWidth = MAX_SCREEN_WIDTH_DEFAULT; }
        if(widgets == null) { widgets = new DashboardWidget[0]; }
        if(columns > 0 && screenPresets != null && screenPresets.length > 0) {
            if(Arrays.stream(screenPresets).anyMatch(preset -> !preset.checkValidity())) {
                return false; // If a screenPreset is invalid
            }
            if(widgets.length > 0) {
                if(Arrays.stream(widgets).anyMatch(widget -> !widget.checkValidity())) {
                    return false; // If a widget is invalid
                }
            }
            // TODO: More validity checks
            return true;
        }
        return false;
    }
}
