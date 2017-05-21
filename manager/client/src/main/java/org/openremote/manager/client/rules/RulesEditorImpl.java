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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Provider;
import elemental.client.Browser;
import elemental.html.AnchorElement;
import elemental.html.Blob;
import elemental.html.FileReader;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelector;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.*;

import javax.inject.Inject;
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

    FormButton rulesFileDownload;
    @UiField
    LabelElement rulesFileUploadLabel;
    @UiField
    FileUpload rulesFileUpload;

    @UiField
    FormGroup optionsGroup;
    @UiField
    FormCheckBox enabledCheckBox;

    @UiField(provided = true)
    AssetSelector templateAssetSelector;

    @UiField
    FormGroup rulesGroup;
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
    public RulesEditorImpl(Provider<Confirmation> confirmationDialogProvider,
                           AssetBrowser assetBrowser,
                           ManagerMessages managerMessages) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        templateAssetSelector = new AssetSelector(
            assetBrowser.getPresenter(),
            managerMessages,
            managerMessages.templateAsset(),
            managerMessages.selectAssetDescription(),
            true,
            treeNode -> {
                if (presenter != null) {
                    presenter.onTemplateAssetSelection(treeNode);
                }
            }
        ) {
            @Override
            public void beginSelection() {
                RulesEditorImpl.this.setOpaque(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                RulesEditorImpl.this.setOpaque(false);
            }
        };

        RulesEditorImpl.UI ui = GWT.create(RulesEditorImpl.UI.class);
        initWidget(ui.createAndBindUi(this));

        rulesFileDownload.addClickHandler(event -> {
            AnchorElement downloadAnchor = (AnchorElement) Browser.getDocument().createElement("a");
            downloadAnchor.setDownload(
                getName() != null && getName().length() > 0 ? getName() + ".drl.txt" : "OpenRemote-Rules.drl.txt"
            );
            downloadAnchor.setHref(Browser.encodeURI("data:text/plain," + getRules()));
            Browser.getDocument().getBody().appendChild(downloadAnchor);
            downloadAnchor.click();
            Browser.getDocument().getBody().removeChild(downloadAnchor);
        });

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
    public void setTemplateAssetNode(AssetTreeNode assetTreeNode) {
        templateAssetSelector.setSelectedNode(assetTreeNode);
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
        nameGroup.setOpaque(opaque);
        optionsGroup.setOpaque(opaque);
        rulesGroup.setOpaque(opaque);
        submitButtonGroup.setOpaque(opaque);
    }

}
