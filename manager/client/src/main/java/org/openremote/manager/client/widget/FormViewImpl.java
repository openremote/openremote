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
package org.openremote.manager.client.widget;

import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;

public class FormViewImpl extends Composite implements FormView {

    protected final Provider<Confirmation> confirmationDialogProvider;

    @UiField
    public WidgetStyle widgetStyle;

    @UiField
    public ManagerMessages managerMessages;

    @UiField
    public Form form;

    public FormMessages formMessagesSuccess;
    public FormMessages formMessagesError;

    public FormViewImpl(Provider<Confirmation> confirmationDialogProvider, WidgetStyle widgetStyle) {
        this.confirmationDialogProvider = confirmationDialogProvider;

        this.formMessagesSuccess = new FormMessages(widgetStyle, true);
        this.formMessagesError = new FormMessages(widgetStyle, false);

        formMessagesSuccess.setVisible(false);
        formMessagesError.setVisible(false);
    }

    @Override
    public void setFormBusy(boolean busy) {
        form.setBusy(busy);
    }

    @Override
    public void addFormMessageError(String message) {
        formMessagesError.appendMessage(form, message);
    }

    @Override
    public void addFormMessageSuccess(String message) {
        formMessagesSuccess.appendMessage(form, message);
    }

    @Override
    public void clearFormMessagesError() {
        formMessagesError.clear();
    }

    @Override
    public void clearFormMessagesSuccess() {
        formMessagesSuccess.clear();
    }

    @Override
    public void clearFormMessages() {
        clearFormMessagesSuccess();
        clearFormMessagesError();
    }

    @Override
    public void showConfirmation(String title, String text, Runnable onConfirm) {
        showConfirmation(title, text, onConfirm, null);
    }

    @Override
    public void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel) {
        Confirmation confirmation = confirmationDialogProvider.get();
        confirmation.setTitle(title);
        confirmation.setText(text);
        confirmation.setOnConfirm(onConfirm);
        confirmation.setOnCancel(onCancel);
        confirmation.show();
    }
}
