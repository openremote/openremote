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
package org.openremote.app.client.rules;

import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.rest.EntityReader;
import org.openremote.app.client.rest.EntityWriter;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.http.RequestParams;
import org.openremote.model.interop.Consumer;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RulesResource;
import org.openremote.model.interop.Runnable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public abstract class AbstractRulesEditorActivity<T extends Ruleset, PLACE extends RulesEditorPlace>
    extends AssetBrowsingActivity<PLACE>
    implements RulesEditor.Presenter {

    private static final Logger LOG = Logger.getLogger(AbstractRulesEditorActivity.class.getName());

    final protected RulesEditor view;
    final protected RulesResource rulesetResource;
    final protected Consumer<ConstraintViolation[]> validationErrorHandler;

    protected Long rulesetId;
    protected T ruleset;

    @Inject
    public AbstractRulesEditorActivity(Environment environment,
                                       AssetBrowser.Presenter assetBrowserPresenter,
                                       RulesEditor view,
                                       RulesResource rulesetResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.rulesetResource = rulesetResource;

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
        rulesetId = place.getRulesetId();
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        assetBrowserPresenter.clearSelection();

        registrations.add(eventBus.register(
            AssetBrowserSelection.class,
            RulesModule.createDefaultNavigationListener(environment)
        ));

        view.clearFormMessages();
        clearViewFieldErrors();

        view.setFormBusy(true);
        if (rulesetId != null) {
            environment.getApp().getRequests().sendAndReturn(
                getEntityReader(),
                loadRequestConsumer(),
                200,
                this::startEdit
            );
        } else {
            startCreate();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    protected void startCreate() {
        ruleset = newRuleset();
        ruleset.setRules(ruleset.getLang().getEmptyRulesExample());
        clearViewMessages();
        writeToView();
        view.setFormBusy(false);
    }

    protected void startEdit(T ruleset) {
        this.ruleset = ruleset;
        clearViewMessages();
        writeToView();
        view.setFormBusy(false);
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        clearViewMessages();
        readFromView();
        environment.getApp().getRequests().sendWith(
            getEntityWriter(),
            createRequestConsumer(),
            204,
            afterCreateRunnable(),
            validationErrorHandler
        );
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        clearViewMessages();
        readFromView();
        environment.getApp().getRequests().sendWith(
            getEntityWriter(),
            updateRequestConsumer(),
            204,
            afterUpdateRunnable(),
            validationErrorHandler
        );
    }

    @Override
    public void delete() {
        view.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(ruleset.getName()),
            () -> {
                view.setFormBusy(true);
                clearViewMessages();
                environment.getApp().getRequests().send(
                    deleteRequestConsumer(),
                    204,
                    afterDeleteRunnable()
                );
            }
        );
    }

    @Override
    public void onLanguageChange(Ruleset.Lang lang) {
        if (!view.getRules().equals(ruleset.getLang().getEmptyRulesExample())) {
            view.showConfirmation(
                environment.getMessages().confirmation(),
                environment.getMessages().confirmationRulesLangChange(),
                () -> {
                    ruleset.setLang(lang);
                    ruleset.setRules(lang.getEmptyRulesExample());
                    view.setRules(ruleset.getRules());
                },
                () -> view.setLang(ruleset.getLang())
            );
        } else {
            ruleset.setLang(lang);
            ruleset.setRules(lang.getEmptyRulesExample());
            view.setRules(ruleset.getRules());
        }
    }

    protected void writeToView() {
        view.setName(ruleset.getName());
        view.setLang(ruleset.getLang());
        view.setRulesetEnabled(ruleset.isEnabled());
        view.setRules(ruleset.getRules());
        view.enableCreate(rulesetId == null);
        view.enableUpdate(rulesetId != null);
        view.enableDelete(rulesetId != null);
    }

    protected void readFromView() {
        ruleset.setName(view.getName());
        ruleset.setEnabled(view.getRulesetEnabled());
        ruleset.setLang(view.getLang());
        ruleset.setRules(view.getRules());
    }

    protected void clearViewMessages() {
        view.clearFormMessages();
        clearViewFieldErrors();
    }

    protected void clearViewFieldErrors() {
        view.setNameError(false);
    }

    protected Runnable afterCreateRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowSuccessEvent(
                environment.getMessages().rulesetCreated(ruleset.getName())
            ));
            afterCreate();
        };
    }

    protected Runnable afterUpdateRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowSuccessEvent(
                environment.getMessages().rulesetUpdated(ruleset.getName())
            ));
            afterUpdate();
        };
    }

    protected Runnable afterDeleteRunnable() {
        return () -> {
            view.setFormBusy(false);
            environment.getEventBus().dispatch(new ShowSuccessEvent(
                environment.getMessages().rulesetDeleted(ruleset.getName())
            ));
            afterDelete();
        };
    }

    abstract protected T newRuleset();

    abstract protected EntityReader<T> getEntityReader();

    abstract protected Consumer<RequestParams<Void, T>> loadRequestConsumer();

    abstract protected EntityWriter<T> getEntityWriter();

    abstract protected Consumer<RequestParams<T, Void>> createRequestConsumer();

    abstract protected void afterCreate();

    abstract protected Consumer<RequestParams<T, Void>> updateRequestConsumer();

    abstract protected void afterUpdate();

    abstract protected Consumer<RequestParams<Void, Void>> deleteRequestConsumer();

    abstract protected void afterDelete();


}
