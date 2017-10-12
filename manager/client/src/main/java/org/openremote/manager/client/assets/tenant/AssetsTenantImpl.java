/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.assets.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelector;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.security.User;
import org.openremote.model.Constants;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.util.TextUtil;

import javax.inject.Inject;

public class AssetsTenantImpl extends FormViewImpl implements AssetsTenant {

    interface UI extends UiBinder<FlexSplitPanel, AssetsTenantImpl> {
    }

    @UiField
    HTMLPanel sidebarContainer;

    /* ############################################################################ */

    @UiField
    Headline headline;

    @UiField(provided = true)
    AssetSelector assetSelector;

    @UiField
    FormGroup usersListGroup;

    @UiField
    ListBox usersListBox;

    @UiField
    PushButton createAssetLinkButton;

    @UiField
    FlowPanel userAssetsContainer;

    final AssetBrowser assetBrowser;
    Presenter presenter;

    @Inject
    public AssetsTenantImpl(AssetBrowser assetBrowser,
                            Provider<Confirmation> confirmationDialogProvider,
                            Environment environment) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        assetSelector = new AssetSelector(
            assetBrowser.getPresenter(),
            environment.getMessages(),
            environment.getMessages().linkAsset(),
            environment.getMessages().selectAssetDescription(),
            false,
            true,
            treeNode -> {
                if (presenter != null) {
                    presenter.onAssetSelected(treeNode);
                }
            }
        ) {
            @Override
            public void beginSelection() {
                AssetsTenantImpl.this.setDisabled(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                AssetsTenantImpl.this.setDisabled(false);
            }
        };

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        usersListBox.addChangeHandler(event -> {
            String username = usersListBox.getSelectedValue();
            if (presenter != null)
                presenter.onUserSelected(!TextUtil.isNullOrEmpty(username) ? username : null);
        });

        clearUserAssets(false);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Reset state
        sidebarContainer.clear();
        headline.setText(null);
        headline.setSub(managerMessages.linkAssetUsers());
        headline.setVisible(false);

        assetSelector.init();

        usersListBox.clear();
        usersListBox.addItem(managerMessages.loadingDotdotdot());

        createAssetLinkButton.setEnabled(false);
        createAssetLinkButton.addClickHandler(event -> {
            if (presenter != null)
                presenter.onCreateAssetLink();
        });

        clearUserAssets(false);

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setTenantName(String name) {
        headline.setText(name);
        headline.setVisible(true);
    }

    @Override
    public void setUsers(User[] users) {
        usersListBox.clear();
        usersListBox.addItem(managerMessages.noUserSelected(), "");
        if (users.length == 0) {
            usersListBox.addItem(managerMessages.noUserFound(), "");
        }
        for (User user : users) {
            usersListBox.addItem(user.getUsername() + " (" + user.getFirstName() + " " + user.getLastName() + ")", user.getUsername());
        }
    }

    @Override
    public void setCreateAssetLinkEnabled(boolean enabled) {
        createAssetLinkButton.setEnabled(enabled);
    }


    @Override
    public void setUserAssets(UserAsset[] userAssets) {
        clearUserAssets(userAssets.length == 0);
        for (UserAsset userAsset : userAssets) {
            userAssetsContainer.add(new UserAssetItem(userAsset));
        }
    }

    @Override
    public void removeUserAsset(UserAsset.Id id) {
        for (int i = 0; i < userAssetsContainer.getWidgetCount(); i++) {
            if (userAssetsContainer.getWidget(i) instanceof UserAssetItem ) {
                UserAssetItem item = (UserAssetItem) userAssetsContainer.getWidget(i);
                if (item.getUserAsset().getId().equals(id))
                    userAssetsContainer.remove(i);
            }
        }
        clearUserAssets(userAssetsContainer.getWidgetCount() == 0);
    }

    protected void clearUserAssets(boolean addEmptyMessage) {
        userAssetsContainer.clear();
        if (addEmptyMessage) {
            Label emptyLabel = new Label(managerMessages.noAssetUserLinks());
            emptyLabel.addStyleName(widgetStyle.FormListEmptyMessage());
            userAssetsContainer.add(emptyLabel);
        }
    }

    protected void setDisabled(boolean disabled) {
        usersListGroup.setDisabled(disabled);
    }

    protected class UserAssetItem extends FlowPanel {

        private final UserAsset userAsset;

        public UserAssetItem(UserAsset userAsset) {
            this.userAsset = userAsset;
            setStyleName("flex-none layout vertical or-FormListItem");

            FormGroup assetGroup = new FormGroup();
            assetGroup.setAlignStart(true);
            assetGroup.setFromGroupActions(new FormGroupActions());
            FormField assetField = new FormField();
            FormLabel assetLabel = new FormLabel(managerMessages.asset());
            assetGroup.setFormLabel(assetLabel);
            assetGroup.setFormField(assetField);
            FlowPanel namesPanel = new FlowPanel();
            namesPanel.setStyleName("flex layout vertical");
            assetField.add(namesPanel);
            FormOutputText parentAssetNameText = new FormOutputText(userAsset.getParentAssetName());
            namesPanel.add(parentAssetNameText);
            FormOutputText assetNameText = new FormOutputText(userAsset.getAssetName());
            namesPanel.add(assetNameText);
            add(assetGroup);

            FormButton deleteButton = new FormButton();
            deleteButton.setDanger(true);
            deleteButton.setIcon("trash-o");
            deleteButton.setText(managerMessages.deleteLink());
            deleteButton.addClickHandler(event -> {
                if (presenter != null)
                    presenter.onDeleteAssetLink(userAsset.getId());
            });
            assetGroup.getFormGroupActions().add(deleteButton);

            FormGroup userGroup = new FormGroup();
            FormField userField = new FormField();
            FormLabel userLabel = new FormLabel(managerMessages.linkedToUser());
            userGroup.setFormLabel(userLabel);
            userGroup.setFormField(userField);
            FormOutputText userTxt = new FormOutputText(userAsset.getUserFullName());
            userField.add(userTxt);
            add(userGroup);

            FormGroup createdOnGroup = new FormGroup();
            FormField createdOnField = new FormField();
            FormLabel createdOnLabel = new FormLabel(managerMessages.createdOn());
            createdOnGroup.setFormLabel(createdOnLabel);
            createdOnGroup.setFormField(createdOnField);
            FormOutputText lastLoginOutput = new FormOutputText(
                DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(userAsset.getCreatedOn())
            );
            createdOnField.add(lastLoginOutput);
            add(createdOnGroup);
        }

        public UserAsset getUserAsset() {
            return userAsset;
        }
    }

}
