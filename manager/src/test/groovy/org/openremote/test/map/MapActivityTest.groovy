package org.openremote.test.map

import com.google.gwt.place.shared.PlaceController
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.web.bindery.event.shared.EventBus
import elemental.json.JsonObject
import org.openremote.manager.client.presenter.MapActivity
import org.openremote.manager.client.service.RequestServiceImpl
import org.openremote.manager.client.service.SecurityService
import org.openremote.manager.client.view.MapView
import org.openremote.manager.shared.map.MapResource
import org.openremote.test.ClientTrait
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.server.Constants.MASTER_REALM

class MapActivityTest extends Specification implements ContainerTrait, ClientTrait {

    def "Initialize map"() {
        given: "The fake client environment"
        def placeController = Mock(PlaceController)
        def eventBus = Mock(EventBus)
        def activityContainer = Mock(AcceptsOneWidget)
        def activityBus = Mock(com.google.gwt.event.shared.EventBus)

        and: "An authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")
        def securityService = Stub(SecurityService) {
            getRealm() >> realm;
            getToken() >> accessTokenResponse.getToken()
            getXsrfToken() >> "TODO: NOT ENABLED" // TODO
        }
        def requestService = new RequestServiceImpl(securityService)

        and: "The map view, resource, and activity"
        def mapView = Mock(MapView) {
            isMapInitialised() >> false
        }
        def mapResource = Stub(MapResource) {
            // This matches all methods with any parameters, of the MapResource class
            _(_) >> { callResourceProxy(realm, getDelegate()) }
        }
        def mapActivity = new MapActivity(
                mapView, mapResource, requestService, placeController, eventBus
        )

        and: "The expected map settings"
        JsonObject mapSettings;

        when: "The activity is started"
        mapActivity.start(activityContainer, activityBus)

        then: "The view should have the activity set as presenter"
        1 * mapView.setPresenter(mapActivity)

        and: "The view should have been initialized"
        1 * mapView.initialiseMap(!null) >> { mapSettings = it[0] }

        and: "The correct map settings must be used"
        mapSettings.getArray("center").length() == 2;
        mapSettings.getArray("maxBounds").length() == 4;
        mapSettings.getNumber("maxZoom") == 18;
        mapSettings.getObject("style") != null;

    }
}
