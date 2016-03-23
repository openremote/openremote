package org.openremote.manager.client.assets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import org.gwtbootstrap3.client.ui.Button;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetDetailViewImpl extends Composite implements AssetDetailView {

    private static final Logger LOG = Logger.getLogger(AssetDetailViewImpl.class.getName());

    interface UI extends UiBinder<ScrollPanel, AssetDetailViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    Button sendMessageButton;

    @UiField
    Label messageLabel;

    @UiField
    Button togglePopup;

    final AssetMapPanel assetMapPanel;

    @Inject
    public AssetDetailViewImpl(AssetMapPanel assetMapPanel) {
        this.assetMapPanel = assetMapPanel;
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @UiHandler("sendMessageButton")
    public void onButtonClick(final ClickEvent event) {
        presenter.sendMessage();
    }

    @Override
    public void setMessageText(String text) {
        messageLabel.setText(text);
    }

    @UiHandler("togglePopup")
    public void onToggleButtonClick(final ClickEvent event) {
        if (assetMapPanel.isShowing()) {
            LOG.info("### HIDING");
            assetMapPanel.hide();
        } else {
            LOG.info("### SHOWING");
            assetMapPanel.show();
        }
    }

}
