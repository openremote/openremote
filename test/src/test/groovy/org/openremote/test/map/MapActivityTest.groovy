package org.openremote.test.map
// TODO Fix this when we do Asset testing
class MapActivityTest { /* extends Specification implements ContainerTrait, GwtClientTrait {

    def "Initialize map"() {
        given: "The fake client environment"
        def placeController = Mock(PlaceController)
        def activityContainer = Mock(AcceptsOneWidget)
        def activityBus = Mock(EventBus)
        def managerMessages = Mock(ManagerMessages)
        def activityRegistrations = []
        def constraintViolationReader = Mock(EntityReader)

        and: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainerWithoutDemoRules(defaultConfig(serverPort), defaultServices())

        and: "An authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), DEMO_ADMIN_PASSWORD, DEMO_ADMIN_PASSWORD_DEFAULT)
        ).token
        def securityService = Stub(SecurityService) {
            getRealm() >> realm
            getToken() >> accessToken
            updateToken(_, _, _) >> { int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn ->
               successFn.accept(true)
            };
            getXsrfToken() >> "TODO: NOT ENABLED" // TODO
        }
        def requestService = new RequestServiceImpl(securityService, constraintViolationReader)

        and: "a test client target"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm);

        and: "The map view, resource, and activity"
        def mapView = Mock(MapView) {
            isMapInitialised() >> false
        }
        def assetBrowserPresenter = Mock(AssetBrowser.Presenter)
        def mapResource = Stub(MapResource) {
            // This matches all methods with any parameters, of the MapResource class
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        def jsonObjectMapper = new JsonObjectMapper();

        def mapActivity = new MapActivity(
                assetBrowserPresenter, mapView, mapResource, managerMessages, requestService, placeController, jsonObjectMapper
        )

        and: "The expected map settings"
        JsonObject mapSettings;

        when: "The activity is started"
        mapActivity.start(activityContainer, activityBus, activityRegistrations)

        then: "The view should have the activity set as presenter"
        1 * mapView.setPresenter(mapActivity)

        and: "The view should have been initialized"
        1 * mapView.initialiseMap(!null) >> { mapSettings = it[0] }

        and: "The correct map settings must be used"
        mapSettings.getArray("center").length() == 2;
        mapSettings.getArray("maxBounds").length() == 4;
        mapSettings.getNumber("maxZoom") == 18;
        mapSettings.getObject("style") != null;

        cleanup: "the server should be stopped"
        stopContainer(container);
    }
    */
}
