package org.openremote.model.dashboard;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class DashboardTemplate {

    // Fields
    protected String id;

    @Min(value = 1, message = "${Dashboard.template.columns.Min}")
    protected int columns;

    @Min(value = 1, message = "${Dashboard.template.maxScreenWidth.Min}")
    protected int maxScreenWidth;

    @NotNull(message = "{Dashboard.template.screenPresets.NotNull}")
    @Valid
    protected DashboardScreenPreset[] screenPresets;

    @Valid
    protected DashboardWidget[] widgets;



    /* -------------------- */

    public void setId(String id) { this.id = id; }
    public void setColumns(int columns) { this.columns = columns; }
    public void setMaxScreenWidth(int maxScreenWidth) { this.maxScreenWidth = maxScreenWidth; }
    public void setScreenPresets(DashboardScreenPreset[] screenPresets) { this.screenPresets = screenPresets; }
    public void setWidgets(DashboardWidget[] widgets) { this.widgets = widgets; }

    public String getId() { return id; }
    public int getColumns() { return columns; }
    public int getMaxScreenWidth() { return maxScreenWidth; }
    public DashboardScreenPreset[] getScreenPresets() { return screenPresets; }
    public DashboardWidget[] getWidgets() { return widgets; }
}
