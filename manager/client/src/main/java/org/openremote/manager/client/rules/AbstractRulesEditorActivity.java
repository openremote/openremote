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
package org.openremote.manager.client.rules;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.rules.RulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.model.Consumer;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public abstract class AbstractRulesEditorActivity<T extends RulesDefinition, PLACE extends RulesEditorPlace>
    extends AssetBrowsingActivity<PLACE>
    implements RulesEditor.Presenter {

    final protected RulesEditor view;
    final protected RulesResource rulesResource;
    final protected Consumer<ConstraintViolation[]> validationErrorHandler;

    protected Long definitionId;
    protected T rulesDefinition;

    @Inject
    public AbstractRulesEditorActivity(Environment environment,
                                       AssetBrowser.Presenter assetBrowserPresenter,
                                       RulesEditor view,
                                       RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
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
    protected AppActivity<PLACE> init(PLACE place) {
        definitionId = place.getDefinitionId();
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

        view.clearFormMessages();
        clearViewFieldErrors();

        if (definitionId != null) {
            environment.getRequestService().execute(
                getEntityReader(),
                loadRequestConsumer(),
                200,
                rulesDefinition -> {
                    this.rulesDefinition = rulesDefinition;
                    writeToView();
                },
                ex -> handleRequestException(ex, environment)
            );
        } else {
            rulesDefinition = newDefinition();
            writeToView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            getEntityWriter(),
            createRequestConsumer(),
            204,
            afterCreateRunnable(),
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            getEntityWriter(),
            updateRequestConsumer(),
            204,
            afterUpdateRunnable(),
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
                    deleteRequestConsumer(),
                    204,
                    afterDeleteRunnable(),
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

    protected Runnable afterCreateRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowInfoEvent(
                environment.getMessages().rulesetCreated(rulesDefinition.getName())
            ));
            afterCreate();
        };
    }

    protected Runnable afterUpdateRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowInfoEvent(
                environment.getMessages().rulesetUpdated(rulesDefinition.getName())
            ));
            afterUpdate();
        };
    }

    protected Runnable afterDeleteRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowInfoEvent(
                environment.getMessages().rulesetDeleted(rulesDefinition.getName())
            ));
            afterDelete();
        };
    }

    abstract protected T newDefinition();

    abstract protected EntityReader<T> getEntityReader();

    abstract protected Consumer<RequestParams<T>> loadRequestConsumer();

    abstract protected EntityWriter<T> getEntityWriter();

    abstract protected Consumer<RequestParams<Void>> createRequestConsumer();

    abstract protected void afterCreate();

    abstract protected Consumer<RequestParams<Void>> updateRequestConsumer();

    abstract protected void afterUpdate();

    abstract protected Consumer<RequestParams<Void>> deleteRequestConsumer();

    abstract protected void afterDelete();


}
