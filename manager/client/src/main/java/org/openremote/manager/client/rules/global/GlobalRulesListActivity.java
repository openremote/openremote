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
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.RulesModule;
import org.openremote.manager.shared.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.RulesetResource;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class GlobalRulesListActivity
    extends AssetBrowsingActivity<GlobalRulesListPlace>
    implements GlobalRulesList.Presenter {

    final GlobalRulesList view;
    final GlobalRulesetArrayMapper globalRulesetArrayMapper;
    final RulesetResource rulesetResource;

    @Inject
    public GlobalRulesListActivity(Environment environment,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   GlobalRulesList view,
                                   GlobalRulesetArrayMapper globalRulesetArrayMapper,
                                   RulesetResource rulesetResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.globalRulesetArrayMapper = globalRulesetArrayMapper;
        this.rulesetResource = rulesetResource;
    }

    @Override
    protected AppActivity<GlobalRulesListPlace> init(GlobalRulesListPlace place) {
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

        environment.getRequestService().execute(
            globalRulesetArrayMapper,
            rulesetResource::getGlobalRulesets,
            200,
            view::setRulesets,
            ex -> handleRequestException(ex, environment)
        );

        assetBrowserPresenter.clearSelection();
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void onRulesetSelected(GlobalRuleset ruleset) {
        environment.getPlaceController().goTo(new GlobalRulesEditorPlace(ruleset.getId()));
    }

    @Override
    public void createRule() {
        environment.getPlaceController().goTo(new GlobalRulesEditorPlace());
    }
}
