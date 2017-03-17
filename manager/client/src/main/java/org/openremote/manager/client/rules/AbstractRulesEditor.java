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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Provider;
import elemental.client.Browser;
import elemental.html.Blob;
import elemental.html.FileReader;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.widget.*;

public abstract class AbstractRulesEditor<P extends RulesEditor.Presenter>
    extends FormViewImpl
    implements RulesEditor<P> {

    @UiField
    public HTMLPanel sidebarContainer;

    @UiField
    public FormGroup nameGroup;
    @UiField
    public FormInputText nameInput;

    @UiField
    public LabelElement rulesFileUploadLabel;
    @UiField
    public FileUpload rulesFileUpload;

    @UiField
    public FormGroup enabledGroup;
    @UiField
    public FormCheckBox enabledCheckBox;

    @UiField
    public FormGroup rulesGroup;
    @UiField
    public FormTextArea rulesTextArea;

    @UiField
    public FormGroup submitButtonGroup;
    @UiField
    public PushButton createButton;
    @UiField
    public PushButton updateButton;
    @UiField
    public PushButton deleteButton;

    final AssetBrowser assetBrowser;
    protected P presenter;

    public AbstractRulesEditor(Provider<ConfirmationDialog> confirmationDialogProvider, AssetBrowser assetBrowser) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        initComposite();

        rulesFileUpload.addChangeHandler(event -> {
            rulesFileUploadLabel.removeClassName("error");
            JsArray files = (JsArray) rulesFileUpload.getElement().getPropertyJSO("files");
            if (files.length() != 1)
                return;
            Blob file = (Blob) files.get(0);
            if (file.getType().matches("text.*") || rulesFileUpload.getFilename().endsWith(".drl")) {
                final FileReader reader = Browser.getWindow().newFileReader();
                reader.setOnloadend(evt -> setRules(reader.getResult().toString()));
                reader.readAsText(file, "UTF-8");
            } else {
                if (!rulesFileUploadLabel.hasClassName("error")) {
                    rulesFileUploadLabel.addClassName("error");
                    Browser.getWindow().setTimeout(() -> {
                        rulesFileUploadLabel.removeClassName("error");
                    }, 1000);
                }
            }
        });
    }

    abstract protected void initComposite();

    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        } else {
            sidebarContainer.clear();

            // Restore initial state of view
            nameGroup.setError(false);
            nameInput.setValue(null);
            enabledCheckBox.setValue(true);
            rulesTextArea.setValue(null);
            createButton.setVisible(false);
            updateButton.setVisible(false);
            deleteButton.setVisible(false);
        }
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

}
