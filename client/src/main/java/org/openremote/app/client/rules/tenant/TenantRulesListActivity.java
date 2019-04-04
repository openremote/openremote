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
package org.openremote.app.client.rules.tenant;

import org.openremote.app.client.Environment;
import org.openremote.app.client.TenantMapper;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.rules.RulesEngineInfoMapper;
import org.openremote.app.client.rules.RulesModule;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.rules.RulesEngineStatusEvent;
import org.openremote.model.rules.RulesResource;
import org.openremote.model.rules.RulesetChangedEvent;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.security.TenantResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class TenantRulesListActivity
    extends AssetBrowsingActivity<TenantRulesListPlace>
    implements TenantRulesList.Presenter {

    final TenantRulesList view;
    final TenantMapper tenantMapper;
    final RulesEngineInfoMapper rulesEngineInfoMapper;
    final TenantResource tenantResource;
    final TenantRulesetArrayMapper tenantRulesetArrayMapper;
    final RulesResource rulesResource;

    String realm;

    @Inject
    public TenantRulesListActivity(Environment environment,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   TenantRulesList view,
                                   TenantMapper tenantMapper,
                                   RulesEngineInfoMapper rulesEngineInfoMapper,
                                   TenantResource tenantResource,
                                   TenantRulesetArrayMapper tenantRulesetArrayMapper,
                                   RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.tenantMapper = tenantMapper;
        this.rulesEngineInfoMapper = rulesEngineInfoMapper;
        this.tenantResource = tenantResource;
        this.tenantRulesetArrayMapper = tenantRulesetArrayMapper;
        this.rulesResource = rulesResource;
    }

    @Override
    protected AppActivity<TenantRulesListPlace> init(TenantRulesListPlace place) {
        this.realm = place.getRealm();
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

        if (realm != null) {

            subscribeStatusEvents(true, registrations);

            assetBrowserPresenter.selectRealm(realm);

            environment.getApp().getRequests().sendAndReturn(
                tenantMapper,
                params -> tenantResource.get(params, realm),
                200,
                tenant -> {
                    view.setRealmLabel(tenant.getDisplayName());
                    view.setCreateRulesetHistoryToken(
                        environment.getPlaceHistoryMapper().getToken(new TenantRulesEditorPlace(realm))
                    );
                }
            );

            environment.getApp().getRequests().sendAndReturn(
                rulesEngineInfoMapper,
                params -> rulesResource.getTenantEngineInfo(params, realm),
                new double[] {200, 204},
                view::onEngineStatusChanged
            );

            environment.getApp().getRequests().sendAndReturn(
                tenantRulesetArrayMapper,
                params -> rulesResource.getTenantRulesets(params, realm, null, false),
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
    public void onRulesetSelected(TenantRuleset ruleset) {
        environment.getPlaceController().goTo(new TenantRulesEditorPlace(realm, ruleset.getId()));
    }

    protected void subscribeStatusEvents(boolean subscribe, Collection<EventRegistration> registrations) {
        if (subscribe) {
            environment.getEventService().subscribe(RulesEngineStatusEvent.class);
            environment.getEventService().subscribe(RulesetChangedEvent.class);

            registrations.add(environment.getEventBus().register(
                RulesEngineStatusEvent.class,
                e -> {
                    if (realm.equals(e.getEngineId())) {
                        view.onEngineStatusChanged(e.getEngineInfo());
                    }
                }
            ));

            registrations.add(environment.getEventBus().register(
                RulesetChangedEvent.class,
                e -> {
                    if (realm.equals(e.getEngineId())) {
                        view.onRulesetStatusChanged((TenantRuleset) e.getRuleset());
                    }
                }
            ));
        } else {
            environment.getEventService().unsubscribe(RulesetChangedEvent.class);
            environment.getEventService().unsubscribe(RulesEngineStatusEvent.class);
        }
    }
}