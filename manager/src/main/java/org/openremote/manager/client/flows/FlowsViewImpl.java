package org.openremote.manager.client.flows;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.openremote.manager.client.util.Timeout;

import javax.inject.Inject;
import java.util.logging.Logger;

public class FlowsViewImpl implements FlowsView {

    private static final Logger LOG = Logger.getLogger(FlowsViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, FlowsViewImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private HTMLPanel root;

    Presenter presenter;

    @UiField
    IFrameElement frame;

    @Inject
    public FlowsViewImpl() {
        root = ui.createAndBindUi(this);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // TODO ouch
        Timeout.debounce("setiframefocus", () -> {
            frame.focus();
        }, 1000);
    }

    @Override
    public Widget asWidget() {
        return root;
    }
}
