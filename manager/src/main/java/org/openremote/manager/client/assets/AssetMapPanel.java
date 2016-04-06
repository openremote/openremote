package org.openremote.manager.client.assets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.PopupPanel;
import org.openremote.manager.client.widget.AppPanel;

public class AssetMapPanel implements AppPanel {

    interface UI extends UiBinder<PopupPanel, AssetMapPanel> {
    }

    final PopupPanel popupPanel;

    private UI ui = GWT.create(UI.class);

    public AssetMapPanel() {
        this.popupPanel = ui.createAndBindUi(this);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                popupPanel.center();
                hide();
            }
        });
        popupPanel.hide();
    }

    @Override
    public boolean isShowing() {
        return popupPanel.isShowing();
    }

    @Override
    public void show() {
        popupPanel.show();
    }

    @Override
    public void hide() {
        popupPanel.hide();
    }
}
