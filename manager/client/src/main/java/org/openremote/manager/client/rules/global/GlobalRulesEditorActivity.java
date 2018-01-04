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

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.mvp.AcceptsView;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.manager.client.rules.AbstractRulesEditorActivity;
import org.openremote.manager.client.rules.RulesEditor;
import org.openremote.components.client.rest.EntityReader;
import org.openremote.components.client.rest.EntityWriter;
import org.openremote.model.http.RequestParams;
import org.openremote.model.interop.Consumer;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.RulesetResource;

import javax.inject.Inject;
import java.util.Collection;

public class GlobalRulesEditorActivity
    extends AbstractRulesEditorActivity<GlobalRuleset, GlobalRulesEditorPlace> {

    final GlobalRulesetMapper globalRulesetMapper;

    @Inject
    public GlobalRulesEditorActivity(Environment environment,
                                     Tenant currentTenant,
                                     AssetBrowser.Presenter assetBrowserPresenter,
                                     RulesEditor view,
                                     RulesetResource rulesetResource,
                                     GlobalRulesetMapper globalRulesetMapper) {
        super(environment, currentTenant, assetBrowserPresenter, view, rulesetResource);
        this.globalRulesetMapper = globalRulesetMapper;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        view.setHeadline(environment.getMessages().editGlobalRules(), null);
        view.setFormBusy(false);
    }

    @Override
    protected GlobalRuleset newRuleset() {
        return new GlobalRuleset();
    }

    @Override
    protected EntityReader<GlobalRuleset> getEntityReader() {
        return globalRulesetMapper;
    }

    @Override
    protected Consumer<RequestParams<Void, GlobalRuleset>> loadRequestConsumer() {
        return params -> rulesetResource.getGlobalRuleset(params, rulesetId);
    }

    @Override
    protected EntityWriter<GlobalRuleset> getEntityWriter() {
        return globalRulesetMapper;
    }

    @Override
    protected Consumer<RequestParams<GlobalRuleset, Void>> createRequestConsumer() {
        return params -> rulesetResource.createGlobalRuleset(params, ruleset);
    }

    @Override
    protected void afterCreate() {
        environment.getPlaceController().goTo(new GlobalRulesListPlace());
    }

    @Override
    protected Consumer<RequestParams<GlobalRuleset, Void>> updateRequestConsumer() {
        return params -> rulesetResource.updateGlobalRuleset(params, rulesetId, ruleset);
    }

    @Override
    protected void afterUpdate() {
        environment.getPlaceController().goTo(new GlobalRulesEditorPlace(rulesetId));
    }

    @Override
    protected Consumer<RequestParams<Void, Void>> deleteRequestConsumer() {
        return params -> rulesetResource.deleteGlobalRuleset(params, rulesetId);
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
