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
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.AbstractRulesEditorActivity;
import org.openremote.manager.client.rules.RulesEditor;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.rules.AssetRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.model.Consumer;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetRulesEditorActivity
    extends AbstractRulesEditorActivity<AssetRulesDefinition, AssetRulesEditorPlace> {

    final AssetMapper assetMapper;
    final AssetResource assetResource;
    final AssetRulesDefinitionMapper assetRulesDefinitionMapper;

    String assetId;

    @Inject
    public AssetRulesEditorActivity(Environment environment,
                                    AssetBrowser.Presenter assetBrowserPresenter,
                                    RulesEditor view,
                                    RulesResource rulesResource,
                                    AssetMapper assetMapper,
                                    AssetResource assetResource,
                                    AssetRulesDefinitionMapper assetRulesDefinitionMapper) {
        super(environment, assetBrowserPresenter, view, rulesResource);
        this.assetMapper = assetMapper;
        this.assetResource = assetResource;
        this.assetRulesDefinitionMapper = assetRulesDefinitionMapper;
    }

    @Override
    protected AppActivity<AssetRulesEditorPlace> init(AssetRulesEditorPlace place) {
        assetId = place.getAssetId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        environment.getRequestService().execute(
            assetMapper,
            params -> assetResource.get(params, assetId),
            200,
            asset -> view.setHeadline(environment.getMessages().editAssetRuleset(asset.getName())),
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    protected AssetRulesDefinition newDefinition() {
        return new AssetRulesDefinition(assetId);
    }

    @Override
    protected EntityReader<AssetRulesDefinition> getEntityReader() {
        return assetRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<AssetRulesDefinition>> loadRequestConsumer() {
        return params -> rulesResource.getAssetDefinition(params, definitionId);
    }

    @Override
    protected EntityWriter<AssetRulesDefinition> getEntityWriter() {
        return assetRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<Void>> createRequestConsumer() {
        return params -> rulesResource.createAssetDefinition(params, rulesDefinition);
    }

    @Override
    protected void afterCreate() {
        environment.getPlaceController().goTo(new AssetRulesListPlace(assetId));
    }

    @Override
    protected Consumer<RequestParams<Void>> updateRequestConsumer() {
        return params -> rulesResource.updateAssetDefinition(params, definitionId, rulesDefinition);
    }

    @Override
    protected void afterUpdate() {
        environment.getPlaceController().goTo(new AssetRulesEditorPlace(assetId, definitionId));
    }

    @Override
    protected Consumer<RequestParams<Void>> deleteRequestConsumer() {
        return params -> rulesResource.deleteAssetDefinition(params, definitionId);
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
