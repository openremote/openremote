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
package org.openremote.manager.client.admin.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.openremote.manager.client.style.ThemeStyle;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.MessagesIcon;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.validation.ConstraintViolation;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class AdminTenantImpl extends Composite implements AdminTenant {

    interface UI extends UiBinder<HTMLPanel, AdminTenantImpl> {
    }

    private UI ui = GWT.create(UI.class);

    @UiField
    protected WidgetStyle widgetStyle;
    @UiField
    protected ThemeStyle themeStyle;

    @UiField
    DivElement displayNameGrou;
    @UiField
    LabelElement displayNameLabel;
    @UiField
    TextBox displayNameInput;

    @UiField
    DivElement realmGroup;
    @UiField
    LabelElement realmLabel;
    @UiField
    TextBox realmInput;

    @UiField
    LabelElement enabledLabel;
    @UiField
    SimpleCheckBox enabledCheckBox;

    @UiField
    SimplePanel cellTableContainer;

    @UiField
    PushButton updateButton;

    @UiField
    PushButton createButton;

    @UiField
    PushButton deleteButton;

    @UiField
    PushButton cancelButton;

    @UiField
    DivElement form;

    @UiField
    DivElement formMessages;

    protected Presenter presenter;
    protected Tenant tenant;

    @Inject
    public AdminTenantImpl() {
        initWidget(ui.createAndBindUi(this));

        displayNameLabel.setHtmlFor(Document.get().createUniqueId());
        displayNameInput.getElement().setId(displayNameLabel.getHtmlFor());

        realmLabel.setHtmlFor(Document.get().createUniqueId());
        realmInput.getElement().setId(realmLabel.getHtmlFor());

        enabledLabel.setHtmlFor(Document.get().createUniqueId());
        enabledCheckBox.getElement().setId(enabledLabel.getHtmlFor());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        this.tenant = null;
        writeForm();
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
        writeForm();
    }

    @Override
    public void showErrors(ConstraintViolation[] violations) {
        clearFormMessages();
        List<String> errorMessages = new ArrayList<>();
        for (ConstraintViolation violation : violations) {
            errorMessages.add(violation.getMessage());

            if (violation.getPath().endsWith("displayName")) {
                displayNameGrou.addClassName("error");
            }
            if (violation.getPath().endsWith("realm")) {
                realmGroup.addClassName("error");
            }

        }
        showFormMessages(false, errorMessages.toArray(new String[errorMessages.size()]));
    }

    @Override
    public void showSuccess(String message) {
        clearFormMessages();
        showFormMessages(true, message);
    }

    @UiHandler("updateButton")
    void updateClicked(ClickEvent e) {
        readForm();
        setFormBusy(true);
        presenter.updateTenant(tenant, () -> setFormBusy(false));
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        readForm();
        setFormBusy(true);
        presenter.createTenant(tenant, () -> setFormBusy(false));
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        setFormBusy(true);
        presenter.deleteTenant(tenant, () -> {
            tenant = null;
            writeForm();
            setFormBusy(false);
        });
    }

    @UiHandler("cancelButton")
    void cancelClicked(ClickEvent e) {
        tenant = null;
        writeForm();
        presenter.cancel();
    }

    void writeForm() {
        clearFormMessages();

        updateButton.setVisible(false);
        deleteButton.setVisible(false);
        createButton.setVisible(false);
        if (tenant != null) {
            if (tenant.getId() != null) {
                updateButton.setVisible(true);
                deleteButton.setVisible(true);
            } else {
                createButton.setVisible(true);
            }
            displayNameInput.setText(tenant.getDisplayName());
            realmInput.setText(tenant.getRealm());
            enabledCheckBox.setValue(tenant.getEnabled());
        } else {
            displayNameInput.setText(null);
            enabledCheckBox.setValue(false);
        }
    }

    void readForm() {
        tenant.setDisplayName(displayNameInput.getText().length() > 0 ? displayNameInput.getText() : null);
        tenant.setRealm(realmInput.getText().length() > 0 ? realmInput.getText() : null);
        tenant.setEnabled(enabledCheckBox.getValue());
    }

    void setFormBusy(boolean busy) {
        if (busy) {
            form.addClassName(widgetStyle.FormBusy());
            form.addClassName(themeStyle.FormBusy());
        } else {
            form.removeClassName(widgetStyle.FormBusy());
            form.removeClassName(themeStyle.FormBusy());
        }
    }

    void showFormMessages(boolean success, String... messages) {
        if (messages == null || messages.length == 0)
            return;
        formMessages.getStyle().clearDisplay();
        formMessages.addClassName(success ? "success": "error");
        formMessages.appendChild(new MessagesIcon(success).getElement());
        FlowPanel messagesPanel = new FlowPanel();
        formMessages.appendChild(messagesPanel.getElement());
        for (String message : messages) {
            messagesPanel.add(new InlineLabel(message));
            messagesPanel.getElement().appendChild(Document.get().createBRElement());
        }
    }

    void clearFormMessages() {
        formMessages.getStyle().setDisplay(Style.Display.NONE);
        formMessages.removeClassName("error");
        formMessages.removeClassName("success");
        formMessages.removeAllChildren();

        displayNameGrou.removeClassName("error");
        realmGroup.removeClassName("error");
    }
}
