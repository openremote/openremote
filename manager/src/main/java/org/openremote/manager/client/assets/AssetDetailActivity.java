package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.MessageReceivedEvent;
import org.openremote.manager.client.event.ServerSendEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.event.Message;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AssetDetailActivity
        extends AppActivity<AssetsPlace>
        implements AssetDetailView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetDetailActivity.class.getName());

    final AssetDetailView view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetDetailActivity(AssetDetailView view,
                               PlaceController placeController,
                               EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    protected void init(AssetsPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(MessageReceivedEvent.class, event -> {
            view.setMessageText(event.getMessage().getBody());
        }));
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void sendMessage() {
        eventBus.dispatch(new ServerSendEvent(
            new Message("Hello from client!")
        ));
    }
}
