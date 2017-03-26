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
package org.openremote.manager.client.rules.global;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.rules.AbstractRulesEditorActivity;
import org.openremote.manager.client.rules.RulesEditor;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.model.Consumer;

import javax.inject.Inject;
import java.util.Collection;

public class GlobalRulesEditorActivity
    extends AbstractRulesEditorActivity<GlobalRulesDefinition, GlobalRulesEditorPlace> {

    final GlobalRulesDefinitionMapper globalRulesDefinitionMapper;

    @Inject
    public GlobalRulesEditorActivity(Environment environment,
                                     AssetBrowser.Presenter assetBrowserPresenter,
                                     RulesEditor view,
                                     RulesResource rulesResource,
                                     GlobalRulesDefinitionMapper globalRulesDefinitionMapper) {
        super(environment, assetBrowserPresenter, view, rulesResource);
        this.globalRulesDefinitionMapper = globalRulesDefinitionMapper;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        view.setHeadline(environment.getMessages().editGlobalRules());
        view.setFormBusy(false);
    }

    @Override
    protected GlobalRulesDefinition newDefinition() {
        return new GlobalRulesDefinition();
    }

    @Override
    protected EntityReader<GlobalRulesDefinition> getEntityReader() {
        return globalRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<GlobalRulesDefinition>> loadRequestConsumer() {
        return params -> rulesResource.getGlobalDefinition(params, definitionId);
    }

    @Override
    protected EntityWriter<GlobalRulesDefinition> getEntityWriter() {
        return globalRulesDefinitionMapper;
    }

    @Override
    protected Consumer<RequestParams<Void>> createRequestConsumer() {
        return params -> rulesResource.createGlobalDefinition(params, rulesDefinition);
    }

    @Override
    protected void afterCreate() {
        environment.getPlaceController().goTo(new GlobalRulesListPlace());
    }

    @Override
    protected Consumer<RequestParams<Void>> updateRequestConsumer() {
        return params -> rulesResource.updateGlobalDefinition(params, definitionId, rulesDefinition);
    }

    @Override
    protected void afterUpdate() {
        environment.getPlaceController().goTo(new GlobalRulesEditorPlace(definitionId));
    }

    @Override
    protected Consumer<RequestParams<Void>> deleteRequestConsumer() {
        return params -> rulesResource.deleteGlobalDefinition(params, definitionId);
    }

    @Override
    protected void afterDelete() {
        environment.getPlaceController().goTo(new GlobalRulesListPlace());
    }

    @Override
    public void cancel() {
        environment.getPlaceController().goTo(new GlobalRulesListPlace());
    }
}
