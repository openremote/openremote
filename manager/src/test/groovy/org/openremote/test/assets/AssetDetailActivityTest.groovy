package org.openremote.test.assets

import com.google.gwt.place.shared.PlaceController
import com.google.gwt.user.client.ui.AcceptsOneWidget
import org.openremote.manager.client.assets.AssetDetailActivity
import org.openremote.manager.client.assets.AssetDetailView
import org.openremote.manager.client.event.bus.EventBus
import org.openremote.manager.server.event.EventService
import org.openremote.test.ClientTrait
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.server.Constants.MASTER_REALM

class AssetDetailActivityTest extends Specification implements ContainerTrait, ClientTrait {


    def "Initialize map"() {
        given: "The fake client environment"
        def placeController = Mock(PlaceController)
        def activityContainer = Mock(AcceptsOneWidget)
        def activityBus = new EventBus()
        def activityRegistrations = []
        def result = new BlockingVariables(5)

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")

        and: "a connected messaging client"
        def eventEndpoint = new ClientTrait.EventEndpoint(activityBus, getJSON());
        connect(
                eventEndpoint,
                getClientTarget(),
                realm,
                accessTokenResponse.getToken(),
                EventService.WEBSOCKET_EVENTS
        )

        and: "the view, resource, and activity"
        def assetDetailView = Mock(AssetDetailView) {
            setMessageText(_) >> { String value ->
                result.messageText = value;
            }
        }

        def assetDetailActivity = new AssetDetailActivity(
                assetDetailView, placeController, activityBus
        )

        when: "the activity is started"
        assetDetailActivity.start(activityContainer, activityBus, activityRegistrations)

        then: "the view should have the activity set as presenter"
        1 * assetDetailView.setPresenter(assetDetailActivity)

        when: "the client sends a message"
        assetDetailActivity.sendMessage()

        then: "the server should send a message"
        result.messageText.startsWith("Hello from server, the time is")

        and: "the messaging should be closed so we don't get an ugly termination exception on the server"
        eventEndpoint.close()

    }
}
