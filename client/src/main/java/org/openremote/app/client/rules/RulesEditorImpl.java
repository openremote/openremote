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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import elemental.client.Browser;
import elemental.html.AnchorElement;
import elemental.html.Blob;
import elemental.html.FileReader;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.widget.*;
import org.openremote.model.rules.Ruleset;

import javax.inject.Inject;
import java.util.Optional;
import java.util.logging.Logger;

public class RulesEditorImpl extends FormViewImpl implements RulesEditor {

    private static final Logger LOG = Logger.getLogger(RulesEditorImpl.class.getName());

    interface UI extends UiBinder<FlexSplitPanel, RulesEditorImpl> {
    }

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    Headline headline;

    @UiField
    FormGroup nameGroup;
    @UiField
    FormInputText nameInput;
    @UiField
    FormListBox languageListBox;

    @UiField
    FormGroup optionsGroup;
    @UiField
    FormCheckBox enabledCheckBox;
    @UiField
    FormCheckBox continueOnErrorCheckBox;
    @UiField
    FormButton rulesFileDownload;
    @UiField
    FileUploadLabelled rulesFileUpload;

    @UiField
    FormTextArea rulesTextArea;

    @UiField
    FormGroup submitButtonGroup;
    @UiField
    PushButton createButton;
    @UiField
    PushButton updateButton;
    @UiField
    PushButton deleteButton;
    @UiField
    FormButton cancelButton;

    final AssetBrowser assetBrowser;
    protected Presenter presenter;

    @Inject
    public RulesEditorImpl(AssetBrowser assetBrowser,
                           Environment environment) {
        super(environment.getWidgetStyle());
        this.assetBrowser = assetBrowser;

        RulesEditorImpl.UI ui = GWT.create(RulesEditorImpl.UI.class);
        initWidget(ui.createAndBindUi(this));

        for (Ruleset.Lang lang : Ruleset.Lang.values()) {
            languageListBox.addItem(lang.toString());
        }

        languageListBox.addChangeHandler(event -> {
            if (presenter != null) {
                presenter.onLanguageChange(getLang());
            }
        });

        rulesFileDownload.addClickHandler(event -> {
            AnchorElement downloadAnchor = (AnchorElement) Browser.getDocument().createElement("a");
            downloadAnchor.setDownload(
                (getName() != null && getName().length() > 0 ? getName() : "OpenRemote-Rules") + getLang().getFileExtension()
            );
            downloadAnchor.setHref(Browser.encodeURI("data:text/plain," + getRules()));
            Browser.getDocument().getBody().appendChild(downloadAnchor);
            downloadAnchor.click();
            Browser.getDocument().getBody().removeChild(downloadAnchor);
        });

        rulesFileUpload.getFileUpload().addChangeHandler(event -> {
            rulesFileUpload.getElement().removeClassName("error");
            JsArray files = (JsArray) rulesFileUpload.getFileUpload().getElement().getPropertyJSO("files");
            if (files.length() != 1)
                return;
            Blob file = (Blob) files.get(0);
            Optional<Ruleset.Lang> lang = Ruleset.Lang.valueOfFileName(rulesFileUpload.getFileUpload().getFilename());
            if (lang.isPresent()) {
                // TODO Does this work? file.getType().matches("text.*")
                final FileReader reader = Browser.getWindow().newFileReader();
                reader.setOnloadend(evt -> {
                    setLang(lang.get());
                    setRules(reader.getResult().toString());
                });
                reader.readAsText(file, "UTF-8");
            } else {
                if (!rulesFileUpload.getElement().hasClassName("error")) {
                    rulesFileUpload.getElement().addClassName("error");
                    Browser.getWindow().setTimeout(() -> {
                        rulesFileUpload.getElement().removeClassName("error");
                    }, 1000);
                }
            }
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        } else {
            sidebarContainer.clear();

            // Restore initial state of view
            headline.setText(null);
            headline.setSub(null);
            nameGroup.setError(false);
            nameInput.setValue(null);
            enabledCheckBox.setValue(true);
            continueOnErrorCheckBox.setValue(false);
            rulesTextArea.setValue(null);
            createButton.setVisible(false);
            updateButton.setVisible(false);
            deleteButton.setVisible(false);
        }
    }

    @Override
    public void setHeadline(String text, String sub) {
        headline.setText(text);
        headline.setSub(sub);
    }

    @Override
    public void setName(String name) {
        nameInput.setValue(name);
    }

    @Override
    public String getName() {
        return nameInput.getValue().length() > 0 ? nameInput.getValue() : null;
    }

    @Override
    public void setLang(Ruleset.Lang lang) {
        languageListBox.selectItem(lang.toString());
    }

    @Override
    public Ruleset.Lang getLang() {
        return Ruleset.Lang.valueOf(languageListBox.getSelectedValue());
    }

    @Override
    public void setNameError(boolean error) {
        nameGroup.setError(error);
    }

    @Override
    public void setRulesetEnabled(Boolean enabled) {
        enabledCheckBox.setValue(enabled != null ? enabled : false);
    }

    @Override
    public boolean getRulesetEnabled() {
        return enabledCheckBox.getValue();
    }

    @Override
    public void setContinueOnError(Boolean continueOnError) {
        continueOnErrorCheckBox.setValue(continueOnError != null ? continueOnError : false);
    }

    @Override
    public boolean getContinueOnError() {
        return continueOnErrorCheckBox.getValue();
    }

    @Override
    public void setRules(String rules) {
        rulesTextArea.setText(rules);
    }

    @Override
    public String getRules() {
        return rulesTextArea.getText();
    }

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
    }

    @Override
    public void enableDelete(boolean enable) {
        deleteButton.setVisible(enable);
    }

    @UiHandler("updateButton")
    public void updateClicked(ClickEvent e) {
        if (presenter != null)
            presenter.update();
    }

    @UiHandler("createButton")
    public void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.create();
    }

    @UiHandler("deleteButton")
    public void deleteClicked(ClickEvent e) {
        if (presenter != null)
            presenter.delete();
    }

    @UiHandler("cancelButton")
    public void cancelClicked(ClickEvent e) {
        if (presenter != null)
            presenter.cancel();
    }

    protected void setOpaque(boolean opaque) {
        nameGroup.setDisabled(opaque);
        optionsGroup.setDisabled(opaque);
        rulesTextArea.setOpaque(opaque);
        submitButtonGroup.setDisabled(opaque);
    }

}
