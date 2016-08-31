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

import com.google.gwt.dom.client.Document;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.ThemeStyle;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.shared.Runnable;

public class FormViewImpl extends Composite implements FormView {

    protected final Provider<ConfirmationDialog> confirmationDialogProvider;

    @UiField
    public WidgetStyle widgetStyle;

    @UiField
    public ThemeStyle themeStyle;

    @UiField
    public ManagerMessages managerMessages;

    @UiField
    public Form form;

    @UiField
    public FlowPanel formMessagesSuccess;

    @UiField
    public FlowPanel formMessagesError;

    public FormViewImpl(Provider<ConfirmationDialog> confirmationDialogProvider) {
        this.confirmationDialogProvider = confirmationDialogProvider;
    }

    @Override
    public void setFormBusy(boolean busy) {
        form.setBusy(busy);
    }

    @Override
    public void addFormMessageError(String message) {
        formMessagesError.add(new InlineLabel(message));
        formMessagesError.getElement().appendChild(Document.get().createBRElement());
        formMessagesError.getParent().setVisible(true);
    }

    @Override
    public void addFormMessageSuccess(String message) {
        formMessagesSuccess.add(new InlineLabel(message));
        formMessagesSuccess.getElement().appendChild(Document.get().createBRElement());
        formMessagesSuccess.getParent().setVisible(true);
    }

    @Override
    public void clearFormMessagesError() {
        formMessagesError.clear();
        formMessagesError.getParent().setVisible(false);
    }

    @Override
    public void clearFormMessagesSuccess() {
        formMessagesSuccess.clear();
        formMessagesSuccess.getParent().setVisible(false);
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
        ConfirmationDialog confirmationDialog = confirmationDialogProvider.get();
        confirmationDialog.setTitle(title);
        confirmationDialog.setText(text);
        confirmationDialog.setOnConfirm(onConfirm);
        confirmationDialog.setOnCancel(onCancel);
        confirmationDialog.show();
    }
}
