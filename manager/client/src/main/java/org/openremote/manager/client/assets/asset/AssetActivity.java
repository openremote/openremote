/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.assets.asset;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.assets.event.AssetsModifiedEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetActivity
    extends AssetBrowsingActivity<AssetView, AssetPlace>
    implements AssetView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetActivity.class.getName());

    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    protected double[] selectedCoordinates;

    @Inject
    public AssetActivity(Environment environment,
                         AssetView view,
                         AssetBrowser.Presenter assetBrowserPresenter,
                         AssetResource assetResource,
                         AssetMapper assetMapper,
                         MapResource mapResource,
                         JsonObjectMapper jsonObjectMapper) {
        super(environment, view, assetBrowserPresenter, assetResource, assetMapper);
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        if (!getView().isMapInitialised()) {
            environment.getRequestService().execute(
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                view::initialiseMap,
                ex -> handleRequestException(ex, environment)
            );
        } else {
            view.refreshMap();
        }
    }

    @Override
    protected void startCreateAsset() {
        super.startCreateAsset();

        view.setFormBusy(true);

        asset = new Asset();
        asset.setName("My New Asset");
        asset.setType(AssetType.GENERIC);

        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        setViewMode(true);
        view.setFormBusy(false);
    }

    @Override
    protected void onAssetLoaded() {
        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        setViewMode(false);
        view.setFormBusy(false);
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(String selectedAssetId) {
        environment.getPlaceController().goTo(new AssetPlace(selectedAssetId));
    }

    @Override
    protected void onBeforeAssetLoad() {
        view.setFormBusy(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        clearViewFieldErrors();

        view.setPresenter(null);
        view.clearFormMessages();
    }

    @Override
    public void onMapClicked(double lng, double lat) {
        selectedCoordinates = new double[] {lng, lat};
        view.showMapPopup(lng, lat, environment.getMessages().selectedLocation());
        view.setLocation(getLocation(selectedCoordinates));
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> {
                assetResource.update(requestParams, assetId, asset);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().assetUpdated(asset.getName())
                ));
                environment.getEventBus().dispatch(new AssetsModifiedEvent(asset));
                environment.getPlaceController().goTo(new AssetPlace(assetId));
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> {
                assetResource.create(requestParams, asset);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().assetCreated(asset.getName())
                ));
                environment.getEventBus().dispatch(new AssetsModifiedEvent(asset));
                environment.getPlaceController().goTo(new AssetsDashboardPlace());
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void delete() {
        view.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(asset.getName()),
            () -> {
                view.setFormBusy(true);
                view.clearFormMessages();
                clearViewFieldErrors();
                environment.getRequestService().execute(
                    requestParams -> {
                        assetResource.delete(requestParams, this.assetId);
                    },
                    204,
                    () -> {
                        view.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowInfoEvent(
                            environment.getMessages().assetDeleted(asset.getName())
                        ));
                        environment.getEventBus().dispatch(new AssetsModifiedEvent(asset));
                        environment.getPlaceController().goTo(new AssetsDashboardPlace());
                    },
                    ex -> handleRequestException(ex, environment)
                );
            }
        );
    }

    protected void writeToView() {
        view.setName(asset.getName());
        view.setType(asset.getType());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(getLocation(asset.getCoordinates()));
        if (asset != null && asset.getId() != null) {
            view.showFeaturesSelection(getFeature(asset));
            view.flyTo(asset.getCoordinates());
        } else {
            view.hideFeaturesSelection();
        }
    }

    protected void readFromView() {
        asset.setName(view.getName());
        asset.setType(view.getType());
        if (selectedCoordinates != null) {
            asset.setCoordinates(selectedCoordinates);
        }
    }

    protected void clearViewFieldErrors() {
        // TODO: Validation
    }

    protected String getLocation(double[] coordinates) {
        if (coordinates != null && coordinates.length == 2) {
            return coordinates[0] + " " + coordinates[1];
        }
        return environment.getMessages().selectLocation();
    }

    protected void setViewMode(boolean enableCreate) {
        view.enableCreate(enableCreate);
        view.enableUpdate(!enableCreate);
        view.enableDelete(!enableCreate);
    }
}
