/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.client.rules.asset;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.rules.AssetRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.model.Consumer;
import org.openremote.model.asset.Asset;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetRulesListActivity
    extends AssetBrowsingActivity<AssetRulesListPlace>
    implements AssetRulesList.Presenter {

    final AssetRulesList view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetRulesDefinitionArrayMapper assetRulesDefinitionArrayMapper;
    final RulesResource rulesResource;

    String assetId;
    Asset asset;

    @Inject
    public AssetRulesListActivity(Environment environment,
                                  AssetBrowser.Presenter assetBrowserPresenter,
                                  AssetRulesList view,
                                  AssetResource assetResource,
                                  AssetMapper assetMapper,
                                  AssetRulesDefinitionArrayMapper assetRulesDefinitionArrayMapper,
                                  RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetRulesDefinitionArrayMapper = assetRulesDefinitionArrayMapper;
        this.rulesResource = rulesResource;
    }

    @Override
    protected AppActivity<AssetRulesListPlace> init(AssetRulesListPlace place) {
        this.assetId = place.getAssetId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                environment.getPlaceController().goTo(new TenantRulesListPlace(event.getSelectedNode().getRealm()));
            } else if (event.isAssetSelection()) {
                environment.getPlaceController().goTo(new AssetRulesListPlace(event.getSelectedNode().getId()));
            }
        }));

        if (assetId != null) {

            loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                if (asset != null) {
                    assetBrowserPresenter.selectAsset(asset);
                    view.setAssetLabel(asset.getName());
                }
            });

            environment.getRequestService().execute(
                assetRulesDefinitionArrayMapper,
                params -> rulesResource.getAssetDefinitions(params, assetId),
                200,
                view::setRulesDefinitions,
                ex -> handleRequestException(ex, environment)
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void onRulesDefinitionSelected(AssetRulesDefinition definition) {
        // TODO environment.getPlaceController().goTo(new AssetRulesEditorPlace(definition.getId()));
    }

    @Override
    public void createRule() {
        // TODO environment.getPlaceController().goTo(new AssetRulesEditorPlace());
    }

    protected void loadAsset(String id, Consumer<Asset> assetConsumer) {
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, id),
            200,
            assetConsumer,
            ex -> handleRequestException(ex, environment)
        );
    }

}