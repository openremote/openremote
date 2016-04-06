package org.openremote.manager.client.flows;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class FlowsActivity
        extends AppActivity<FlowsPlace>
        implements FlowsView.Presenter {

    private static final Logger LOG = Logger.getLogger(FlowsActivity.class.getName());

    final FlowsView view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public FlowsActivity(FlowsView view,
                         PlaceController placeController,
                         EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    public AppActivity<FlowsPlace> init(FlowsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setWidget(view.asWidget());
        view.setPresenter(this);
    }
}
