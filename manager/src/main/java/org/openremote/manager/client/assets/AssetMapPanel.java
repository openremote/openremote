package org.openremote.manager.client.assets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import org.openremote.manager.client.widget.AppPanel;

public class AssetMapPanel extends Composite implements AppPanel {

    interface UI extends UiBinder<PopupPanel, AssetMapPanel> {
    }

    final PopupPanel popupPanel;

    private UI ui = GWT.create(UI.class);

    public AssetMapPanel() {
        this.popupPanel = ui.createAndBindUi(this);
        initWidget(popupPanel);
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
