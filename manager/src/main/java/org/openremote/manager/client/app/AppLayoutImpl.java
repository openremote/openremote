package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.assets.AssetsPlace;

import javax.inject.Inject;

public class AppLayoutImpl extends Composite implements AppLayout {

    interface UI extends UiBinder<HeaderPanel, AppLayoutImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private HeaderPanel appLayout;

    @UiField
    SimpleLayoutPanel headerPanel;

    @UiField
    SimpleLayoutPanel contentPanel;

    @UiField
    SimpleLayoutPanel leftSidePanel;

    @UiField
    SplitLayoutPanel bodyPanel;

    @UiField(provided = true)
    ManagerConstants constants;

    Presenter presenter;

    EventBus eventBus;

    @Inject
    public AppLayoutImpl(EventBus eventBus, ManagerConstants constants) {
        this.eventBus = eventBus;
        this.constants = constants;
        appLayout = ui.createAndBindUi(this);
        initWidget(appLayout);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public AcceptsOneWidget getMainContentPanel() {
        return contentPanel;
    }

    @Override
    public AcceptsOneWidget getLeftSidePanel() {
        return leftSidePanel;
    }

    @Override
    public AcceptsOneWidget getHeaderPanel() {
        return headerPanel;
    }

    @Override
    public void updateLayout(Place place) {
        bodyPanel.setWidgetHidden(leftSidePanel, true);
        bodyPanel.setWidgetHidden(contentPanel, false);

        if (place instanceof AssetsPlace) {
            bodyPanel.setWidgetHidden(leftSidePanel, false);
        }
    }
}
