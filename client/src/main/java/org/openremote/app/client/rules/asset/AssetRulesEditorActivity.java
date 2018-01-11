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
import org.openremote.app.client.assets.AssetMapper;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.rest.EntityReader;
import org.openremote.app.client.rest.EntityWriter;
import org.openremote.app.client.rules.AbstractRulesEditorActivity;
import org.openremote.app.client.rules.RulesEditor;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.http.RequestParams;
import org.openremote.model.interop.Consumer;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.RulesetResource;

import javax.inject.Inject;
import java.util.Collection;

public class AssetRulesEditorActivity
    extends AbstractRulesEditorActivity<AssetRuleset, AssetRulesEditorPlace> {

    final AssetMapper assetMapper;
    final AssetResource assetResource;
    final AssetRulesetMapper assetRulesetMapper;

    String assetId;

    @Inject
    public AssetRulesEditorActivity(Environment environment,
                                    AssetBrowser.Presenter assetBrowserPresenter,
                                    RulesEditor view,
                                    RulesetResource rulesetResource,
                                    AssetMapper assetMapper,
                                    AssetResource assetResource,
                                    AssetRulesetMapper assetRulesetMapper) {
        super(environment, assetBrowserPresenter, view, rulesetResource);
        this.assetMapper = assetMapper;
        this.assetResource = assetResource;
        this.assetRulesetMapper = assetRulesetMapper;
    }

    @Override
    protected AppActivity<AssetRulesEditorPlace> init(AssetRulesEditorPlace place) {
        assetId = place.getAssetId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        environment.getApp().getRequestService().sendAndReturn(
            assetMapper,
            params -> assetResource.get(params, assetId),
            200,
            asset -> {
                view.setHeadline(asset.getName(), environment.getMessages().editAssetRuleset());
            }
        );
    }

    @Override
    protected AssetRuleset newRuleset() {
        return new AssetRuleset(assetId);
    }

    @Override
    protected EntityReader<AssetRuleset> getEntityReader() {
        return assetRulesetMapper;
    }

    @Override
    protected Consumer<RequestParams<Void, AssetRuleset>> loadRequestConsumer() {
        return params -> rulesetResource.getAssetRuleset(params, rulesetId);
    }

    @Override
    protected EntityWriter<AssetRuleset> getEntityWriter() {
        return assetRulesetMapper;
    }

    @Override
    protected Consumer<RequestParams<AssetRuleset, Void>> createRequestConsumer() {
        return params -> rulesetResource.createAssetRuleset(params, ruleset);
    }

    @Override
    protected void afterCreate() {
        environment.getPlaceController().goTo(new AssetRulesListPlace(assetId));
    }

    @Override
    protected Consumer<RequestParams<AssetRuleset, Void>> updateRequestConsumer() {
        return params -> rulesetResource.updateAssetRuleset(params, rulesetId, ruleset);
    }

    @Override
    protected void afterUpdate() {
        environment.getPlaceController().goTo(new AssetRulesEditorPlace(assetId, rulesetId));
    }

    @Override
    protected Consumer<RequestParams<Void, Void>> deleteRequestConsumer() {
        return params -> rulesetResource.deleteAssetRuleset(params, rulesetId);
    }

    @Override
    protected void afterDelete() {
        environment.getPlaceController().goTo(new AssetRulesListPlace(assetId));
    }

    @Override
    public void cancel() {
        environment.getPlaceController().goTo(new AssetRulesListPlace(assetId));
    }
}
