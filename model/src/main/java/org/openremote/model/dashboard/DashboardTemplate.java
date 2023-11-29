package org.openremote.model.dashboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DashboardTemplate {

    // Fields
    protected String id;

    @Min(value = 1, message = "${Dashboard.template.columns.Min}")
    protected int columns;

    @Min(value = 1, message = "${Dashboard.template.maxScreenWidth.Min}")
    protected int maxScreenWidth;

    @Valid
    protected DashboardRefreshInterval refreshInterval;

    @NotNull(message = "{Dashboard.template.screenPresets.NotNull}")
    @Valid
    protected DashboardScreenPreset[] screenPresets;

    @Valid
    protected DashboardWidget[] widgets;



    /* -------------------- */

    public DashboardTemplate() {

    }
    public DashboardTemplate(DashboardScreenPreset[] screenPresets) {
        this.columns = 1;
        this.maxScreenWidth = 1;
        this.refreshInterval = DashboardRefreshInterval.OFF;
        this.screenPresets = screenPresets;
    }

    public void setId(String id) { this.id = id; }
    public void setColumns(int columns) { this.columns = columns; }
    public void setMaxScreenWidth(int maxScreenWidth) { this.maxScreenWidth = maxScreenWidth; }
    public void setRefreshInterval(DashboardRefreshInterval interval) { this.refreshInterval = interval; }
    public void setScreenPresets(DashboardScreenPreset[] screenPresets) { this.screenPresets = screenPresets; }
    public void setWidgets(DashboardWidget[] widgets) { this.widgets = widgets; }

    public String getId() { return id; }
    public int getColumns() { return columns; }
    public int getMaxScreenWidth() { return maxScreenWidth; }
    public DashboardRefreshInterval getRefreshInterval() { return this.refreshInterval; }
    public DashboardScreenPreset[] getScreenPresets() { return screenPresets; }
    public DashboardWidget[] getWidgets() { return widgets; }
}
