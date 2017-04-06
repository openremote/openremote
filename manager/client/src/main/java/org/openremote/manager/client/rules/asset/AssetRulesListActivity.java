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
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.RulesModule;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.rules.AssetRuleset;
import org.openremote.manager.shared.rules.RulesetResource;
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
    final AssetRulesetArrayMapper assetRulesetArrayMapper;
    final RulesetResource rulesetResource;

    String assetId;
    Asset asset;

    @Inject
    public AssetRulesListActivity(Environment environment,
                                  Tenant currentTenant,
                                  AssetBrowser.Presenter assetBrowserPresenter,
                                  AssetRulesList view,
                                  AssetResource assetResource,
                                  AssetMapper assetMapper,
                                  AssetRulesetArrayMapper assetRulesetArrayMapper,
                                  RulesetResource rulesetResource) {
        super(environment, currentTenant, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetRulesetArrayMapper = assetRulesetArrayMapper;
        this.rulesetResource = rulesetResource;
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

        registrations.add(eventBus.register(
            AssetBrowserSelection.class,
            RulesModule.createDefaultNavigationListener(environment)
        ));

        if (assetId != null) {

            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                if (asset != null) {
                    assetBrowserPresenter.selectAsset(asset);
                    view.setAssetLabel(asset.getName());
                }
            });

            environment.getRequestService().execute(
                assetRulesetArrayMapper,
                params -> rulesetResource.getAssetRulesets(params, assetId),
                200,
                view::setRulesets,
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
    public void onRulesetSelected(AssetRuleset ruleset) {
        environment.getPlaceController().goTo(new AssetRulesEditorPlace(assetId, ruleset.getId()));
    }

    @Override
    public void createRule() {
        environment.getPlaceController().goTo(new AssetRulesEditorPlace(assetId));
    }


}