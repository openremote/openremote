package org.openremote.model.dashboard;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class DashboardWidget {

    // Fields
    protected String id;

    @NotBlank(message = "{Dashboard.widget.displayName.NotBlank}")
    protected String displayName;

    @NotNull(message = "{Dashboard.widget.gridItem.NotNull}")
    @Valid
    protected DashboardGridItem gridItem;

    @NotNull(message = "{Dashboard.widget.widgetTypeId.NotNull}")
    protected String widgetTypeId;

    protected Object widgetConfig;


    /* ------------------------------ */

    public void setId(@NotNull @NotEmpty String id) { this.id = id; }
    public void setDisplayName(@NotNull @NotEmpty String displayName) { this.displayName = displayName; }
    public void setGridItem(@NotNull DashboardGridItem gridItem) { this.gridItem = gridItem; }
    public void setWidgetTypeId(@NotNull String widgetTypeId) { this.widgetTypeId = widgetTypeId; }
    public void setWidgetConfig(Object widgetConfig) { this.widgetConfig = widgetConfig; }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public DashboardGridItem getGridItem() { return gridItem; }
    public String getWidgetTypeId() { return widgetTypeId; }
    public Object getWidgetConfig() { return widgetConfig; }
}
