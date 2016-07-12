package org.openremote.test.assets

class AssetsActivityTest
{/* TODO remove outdated test
        extends Specification implements ContainerTrait {

    def "Send/receive hello messages"() {
        given: "The fake client environment"
        def placeController = Mock(PlaceController)
        def activityContainer = Mock(AcceptsOneWidget)
        def activityBus = new EventBus()
        def activityRegistrations = []
        def result = new BlockingVariables(5)

        and: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(container, realm, MANAGER_CLIENT_ID, "test", "test")

        and: "a test client target"
        def client = createClient(container).build();
        def serverUri = serverUri(serverPort).build();
        def websocketContainer = createWebsocketContainer();
        def clientTarget = WebClient.getTarget(client, serverUri);

        and: "a connected messaging client"
        def eventEndpoint = new ContainerTrait.EventEndpoint(activityBus, container.JSON);
        connect(
                websocketContainer,
                eventEndpoint,
                clientTarget,
                realm,
                accessTokenResponse.getToken(),
                EventService.WEBSOCKET_EVENTS
        )

        and: "the view, resource, and activity"
        def assetDetailView = Mock(AssetsView) {
            setMessageText(_) >> { String value ->
                result.messageText = value;
            }
        }

        def assetDetailActivity = new AssetsActivity(
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

        and: "the server should be stopped"
        stopContainer(container);
    }
*/
}
