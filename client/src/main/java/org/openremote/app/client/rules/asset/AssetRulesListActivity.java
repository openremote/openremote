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
package org.openremote.app.client.rules.asset;

import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.AssetMapper;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.rules.RulesEngineInfoMapper;
import org.openremote.app.client.rules.RulesModule;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.RulesEngineStatusEvent;
import org.openremote.model.rules.RulesResource;
import org.openremote.model.rules.RulesetChangedEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class AssetRulesListActivity
    extends AssetBrowsingActivity<AssetRulesListPlace>
    implements AssetRulesList.Presenter {

    final AssetRulesList view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetRulesetArrayMapper assetRulesetArrayMapper;
    final RulesEngineInfoMapper rulesEngineInfoMapper;
    final RulesResource rulesResource;

    String assetId;
    Asset asset;

    @Inject
    public AssetRulesListActivity(Environment environment,
                                  AssetBrowser.Presenter assetBrowserPresenter,
                                  AssetRulesList view,
                                  AssetResource assetResource,
                                  AssetMapper assetMapper,
                                  AssetRulesetArrayMapper assetRulesetArrayMapper,
                                  RulesEngineInfoMapper rulesEngineInfoMapper,
                                  RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetRulesetArrayMapper = assetRulesetArrayMapper;
        this.rulesEngineInfoMapper = rulesEngineInfoMapper;
        this.rulesResource = rulesResource;
    }

    @Override
    protected AppActivity<AssetRulesListPlace> init(AssetRulesListPlace place) {
        this.assetId = place.getAssetId();
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(
            AssetBrowserSelection.class,
            RulesModule.createDefaultNavigationListener(environment)
        ));

        if (assetId != null) {

            subscribeStatusEvents(true, registrations);

            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                if (asset != null) {
                    assetBrowserPresenter.selectAsset(asset);
                    view.setAssetLabel(asset.getName());
                    view.setCreateRulesetHistoryToken(
                        environment.getPlaceHistoryMapper().getToken(new AssetRulesEditorPlace(assetId))
                    );
                }
            });

            environment.getApp().getRequests().sendAndReturn(
                rulesEngineInfoMapper,
                params -> rulesResource.getTenantEngineInfo(params, assetId),
                new double[] {200, 204},
                view::onEngineStatusChanged
            );

            environment.getApp().getRequests().sendAndReturn(
                assetRulesetArrayMapper,
                params -> rulesResource.getAssetRulesets(params, assetId),
                200,
                results -> view.setRulesets(new ArrayList<>(Arrays.asList(results)))
            );
        }
    }

    @Override
    public void onStop() {
        subscribeStatusEvents(false, null);
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void onRulesetSelected(AssetRuleset ruleset) {
        environment.getPlaceController().goTo(new AssetRulesEditorPlace(assetId, ruleset.getId()));
    }

    protected void subscribeStatusEvents(boolean subscribe, Collection<EventRegistration> registrations) {
        if (subscribe) {
            environment.getEventService().subscribe(RulesEngineStatusEvent.class);
            environment.getEventService().subscribe(RulesetChangedEvent.class);

            registrations.add(environment.getEventBus().register(
                RulesEngineStatusEvent.class,
                e -> {
                    if (assetId.equals(e.getEngineId())) {
                        view.onEngineStatusChanged(e.getEngineInfo());
                    }
                }
            ));

            registrations.add(environment.getEventBus().register(
                RulesetChangedEvent.class,
                e -> {
                    if (assetId.equals(e.getEngineId())) {
                        view.onRulesetStatusChanged((AssetRuleset)e.getRuleset());
                    }
                }
            ));
        } else {
            environment.getEventService().unsubscribe(RulesetChangedEvent.class);
            environment.getEventService().unsubscribe(RulesEngineStatusEvent.class);
        }
    }
}