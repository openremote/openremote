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

public class AssetDetailViewImpl extends Composite implements AssetDetailView {

    interface UI extends UiBinder<ScrollPanel, AssetDetailViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    Button sendMessageButton;

    @UiField
    Label messageLabel;

    @Inject
    public AssetDetailViewImpl() {
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
}
