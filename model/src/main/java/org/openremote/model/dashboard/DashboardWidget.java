package org.openremote.model.dashboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class DashboardWidget {

    // Fields
    protected String id;

    @NotNull(message = "{Dashboard.widget.displayName.NotNull}")
    protected String displayName;

    @NotNull(message = "{Dashboard.widget.gridItem.NotNull}")
    @Valid
    protected DashboardGridItem gridItem;

    @NotNull(message = "{Dashboard.widget.widgetTypeId.NotNull}")
    protected String widgetTypeId;

    protected Object widgetConfig;


    /* ------------------------------ */

    public DashboardWidget setId(@NotNull @NotEmpty String id) {
        this.id = id;
        return this;
    }

    public DashboardWidget setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
        return this;
    }

    public DashboardWidget setGridItem(@NotNull DashboardGridItem gridItem) {
        this.gridItem = gridItem;
        return this;
    }

    public DashboardWidget setWidgetTypeId(@NotNull String widgetTypeId) {
        this.widgetTypeId = widgetTypeId;
        return this;
    }

    public DashboardWidget setWidgetConfig(Object widgetConfig) {
        this.widgetConfig = widgetConfig;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DashboardGridItem getGridItem() {
        return gridItem;
    }

    public String getWidgetTypeId() {
        return widgetTypeId;
    }

    public Object getWidgetConfig() {
        return widgetConfig;
    }
}
