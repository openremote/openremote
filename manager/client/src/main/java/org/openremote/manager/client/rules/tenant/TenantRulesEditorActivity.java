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
package org.openremote.manager.client.rules.tenant;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.admin.TenantMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.AbstractRulesEditorActivity;
import org.openremote.manager.client.rules.RulesEditor;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.manager.shared.rules.TenantRulesDefinition;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.Consumer;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class TenantRulesEditorActivity
    extends AbstractRulesEditorActivity<TenantRulesDefinition, TenantRulesEditorPlace> {

    final TenantMapper tenantMapper;
    final TenantResource tenantResource;
    final TenantRulesDefinitionMapper tenantRulesDefinitionMapper;

    String realmId;

    @Inject
    public TenantRulesEditorActivity(Environment environment,
                                     AssetBrowser.Presenter assetBrowserPresenter,
                                     RulesEditor view,
                                     RulesResource rulesResource,
                                     TenantMapper tenantMapper,
                                     TenantResource tenantResource,
                                     TenantRulesDefinitionMapper tenantRulesDefinitionMapper) {
        super(environment, assetBrowserPresenter, view, rulesResource);
        this.tenantMapper = tenantMapper;
        this.tenantResource = tenantResource;
        this.tenantRulesDefinitionMapper = tenantRulesDefinitionMapper;
    }

    @Override
    protected AppActivity<TenantRulesEditorPlace> init(TenantRulesEditorPlace place) {
        realmId = place.getRealmId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        environment.getRequestService().execute(
            tenantMapper,
            params -> tenantResource.getForRealmId(params, realmId),
            200,
            tenant -> {
                view.setHeadline(environment.getMessages().editTenantRuleset(tenant.getDisplayName()));
                view.setFormBusy(false);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    protected TenantRulesDefinition newDefinition() {
        return new TenantRulesDefinition(realmId);
    }

    @Override
    protected EntityReader<TenantRulesDefinition> getEntityReader() {
        return tenantRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<TenantRulesDefinition>> loadRequestConsumer() {
        return params -> rulesResource.getTenantDefinition(params, definitionId);
    }

    @Override
    protected EntityWriter<TenantRulesDefinition> getEntityWriter() {
        return tenantRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<Void>> createRequestConsumer() {
        return params -> rulesResource.createTenantDefinition(params, rulesDefinition);
    }

    @Override
    protected void afterCreate() {
        environment.getPlaceController().goTo(new TenantRulesListPlace(realmId));
    }

    @Override
    protected Consumer<RequestParams<Void>> updateRequestConsumer() {
        return params -> rulesResource.updateTenantDefinition(params, definitionId, rulesDefinition);
    }

    @Override
    protected void afterUpdate() {
        environment.getPlaceController().goTo(new TenantRulesEditorPlace(realmId, definitionId));
    }

    @Override
    protected Consumer<RequestParams<Void>> deleteRequestConsumer() {
        return params -> rulesResource.deleteTenantDefinition(params, definitionId);
    }

    @Override
    protected void afterDelete() {
        environment.getPlaceController().goTo(new TenantRulesListPlace(realmId));
    }

    @Override
    public void cancel() {
        environment.getPlaceController().goTo(new TenantRulesListPlace(realmId));
    }
}
