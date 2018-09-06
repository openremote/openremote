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
package org.openremote.app.client.rules.global;

import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.rules.RulesEngineInfoMapper;
import org.openremote.app.client.rules.RulesModule;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.rules.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class GlobalRulesListActivity
    extends AssetBrowsingActivity<GlobalRulesListPlace>
    implements GlobalRulesList.Presenter {

    final GlobalRulesList view;
    final GlobalRulesetArrayMapper globalRulesetArrayMapper;
    final RulesResource rulesResource;
    final RulesEngineInfoMapper rulesEngineInfoMapper;

    @Inject
    public GlobalRulesListActivity(Environment environment,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   GlobalRulesList view,
                                   GlobalRulesetArrayMapper globalRulesetArrayMapper,
                                   RulesEngineInfoMapper rulesEngineInfoMapper,
                                   RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.globalRulesetArrayMapper = globalRulesetArrayMapper;
        this.rulesEngineInfoMapper = rulesEngineInfoMapper;
        this.rulesResource = rulesResource;
    }

    @Override
    protected AppActivity<GlobalRulesListPlace> init(GlobalRulesListPlace place) {
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

        subscribeStatusEvents(true, registrations);

        environment.getApp().getRequests().sendAndReturn(
            globalRulesetArrayMapper,
            rulesResource::getGlobalRulesets,
            200,
            results -> view.setRulesets(new ArrayList<>(Arrays.asList(results)))
        );

        environment.getApp().getRequests().sendAndReturn(
            rulesEngineInfoMapper,
            rulesResource::getGlobalEngineInfo,
            new double[] {200, 204},
            view::onEngineStatusChanged
        );

        view.setCreateRulesetHistoryToken(
            environment.getPlaceHistoryMapper().getToken(new GlobalRulesEditorPlace())
        );

        assetBrowserPresenter.clearSelection();
    }

    @Override
    public void onStop() {
        subscribeStatusEvents(false, null);
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void onRulesetSelected(GlobalRuleset ruleset) {
        environment.getPlaceController().goTo(new GlobalRulesEditorPlace(ruleset.getId()));
    }

    protected void subscribeStatusEvents(boolean subscribe, Collection<EventRegistration> registrations) {
        if (subscribe) {
            environment.getEventService().subscribe(RulesEngineStatusEvent.class);
            environment.getEventService().subscribe(RulesetChangedEvent.class);

            registrations.add(environment.getEventBus().register(
                RulesEngineStatusEvent.class,
                e -> {
                    if (e.getEngineId() == null) {
                        view.onEngineStatusChanged(e.getEngineInfo());
                    }
                }
            ));

            registrations.add(environment.getEventBus().register(
                RulesetChangedEvent.class,
                e -> {
                    if (e.getEngineId() == null) {
                        view.onRulesetStatusChanged((GlobalRuleset)e.getRuleset());
                    }
                }
            ));
        } else {
            environment.getEventService().unsubscribe(RulesetChangedEvent.class);
            environment.getEventService().unsubscribe(RulesEngineStatusEvent.class);
        }
    }
}
