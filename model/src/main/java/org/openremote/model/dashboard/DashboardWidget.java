package org.openremote.model.dashboard;

public class DashboardWidget {

    // Fields
    protected String id;
    protected String displayName;
    protected DashboardGridItem gridItem;
    protected DashboardWidgetType widgetType;
    protected DashboardWidgetConfig widgetConfig;

    public DashboardWidget() {

    }

    public boolean checkValidity() {
        if(id != null && !id.isEmpty() && displayName != null && !displayName.isEmpty()) {
            // TODO: more validity checks
            return true;
        }
        return false;
    }
}
