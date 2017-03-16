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
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.asset.AssetRulesListPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.model.Consumer;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class GlobalRulesEditorActivity
    extends AssetBrowsingActivity<GlobalRulesEditorPlace>
    implements GlobalRulesEditor.Presenter {

    final GlobalRulesEditor view;
    final GlobalRulesDefinitionMapper globalRulesDefinitionMapper;
    final RulesResource rulesResource;
    final Consumer<ConstraintViolation[]> validationErrorHandler;

    Long definitionId;
    GlobalRulesDefinition rulesDefinition;

    @Inject
    public GlobalRulesEditorActivity(Environment environment,
                                     AssetBrowser.Presenter assetBrowserPresenter,
                                     GlobalRulesEditor view,
                                     GlobalRulesDefinitionMapper globalRulesDefinitionMapper,
                                     RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.globalRulesDefinitionMapper = globalRulesDefinitionMapper;
        this.rulesResource = rulesResource;

        validationErrorHandler = violations -> {
            for (ConstraintViolation violation : violations) {
                if (violation.getPath() != null) {
                    if (violation.getPath().endsWith("name")) {
                        view.setNameError(true);
                    }
                }
                view.addFormMessageError(violation.getMessage());
            }
            view.setFormBusy(false);
        };
    }

    @Override
    protected AppActivity<GlobalRulesEditorPlace> init(GlobalRulesEditorPlace place) {
        definitionId = place.getDefinitionId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                environment.getPlaceController().goTo(new TenantRulesListPlace(event.getSelectedNode().getRealm()));
            } else if (event.isAssetSelection()) {
                environment.getPlaceController().goTo(new AssetRulesListPlace(event.getSelectedNode().getId()));
            }
        }));

        view.clearFormMessages();
        clearViewFieldErrors();

        if (definitionId != null) {
            environment.getRequestService().execute(
                globalRulesDefinitionMapper,
                params -> rulesResource.getGlobalDefinition(params, definitionId),
                200,
                rulesDefinition -> {
                    this.rulesDefinition = rulesDefinition;
                    writeToView();
                },
                ex -> handleRequestException(ex, environment)
            );
        } else {
            rulesDefinition = new GlobalRulesDefinition();
            writeToView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            globalRulesDefinitionMapper,
            requestParams -> {
                rulesResource.updateGlobalDefinition(requestParams, definitionId, rulesDefinition);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().rulesetUpdated(rulesDefinition.getName())
                ));
                environment.getPlaceController().goTo(new GlobalRulesEditorPlace(definitionId));
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            globalRulesDefinitionMapper,
            requestParams -> {
                rulesResource.createGlobalDefinition(requestParams, rulesDefinition);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().rulesetCreated(rulesDefinition.getName())
                ));
                environment.getPlaceController().goTo(new GlobalRulesListPlace());
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void delete() {
        view.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(rulesDefinition.getName()),
            () -> {
                view.setFormBusy(true);
                view.clearFormMessages();
                clearViewFieldErrors();
                environment.getRequestService().execute(
                    requestParams -> {
                        rulesResource.deleteGlobalDefinition(requestParams, definitionId);
                    },
                    204,
                    () -> {
                        view.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowInfoEvent(
                            environment.getMessages().rulesetDeleted(rulesDefinition.getName())
                        ));
                        environment.getPlaceController().goTo(new GlobalRulesListPlace());
                    },
                    ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
                );
            }
        );
    }

    protected void writeToView() {
        view.setName(rulesDefinition.getName());
        view.setRulesetEnabled(rulesDefinition.isEnabled());
        view.setRules(rulesDefinition.getRules());
        view.enableCreate(definitionId == null);
        view.enableUpdate(definitionId != null);
        view.enableDelete(definitionId != null);
    }

    protected void readFromView() {
        rulesDefinition.setName(view.getName());
        rulesDefinition.setEnabled(view.getRulesetEnabled());
        rulesDefinition.setRules(view.getRules());
    }

    protected void clearViewFieldErrors() {
        view.setNameError(false);
    }

}
