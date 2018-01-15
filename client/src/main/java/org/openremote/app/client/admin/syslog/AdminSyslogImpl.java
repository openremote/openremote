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
package org.openremote.app.client.admin.syslog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Provider;
import org.openremote.app.client.Environment;
import org.openremote.app.client.app.dialog.Confirmation;
import org.openremote.app.client.widget.FormInputText;
import org.openremote.app.client.widget.FormListBox;
import org.openremote.app.client.widget.FormViewImpl;
import org.openremote.model.syslog.SyslogConfig;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;

import javax.inject.Inject;

public class AdminSyslogImpl extends FormViewImpl implements AdminSyslog {

    interface UI extends UiBinder<HTMLPanel, AdminSyslogImpl> {
    }

    Presenter presenter;

    @UiField
    FormListBox storeLevelListBox;

    @UiField
    FormInputText expirationInputText;

    @UiField
    FormListBox expirationListBox;

    @UiField(provided = true)
    SyslogFilter syslogFilter;

    @UiField(provided = true)
    SyslogItems syslogItems;

    int limit = SyslogConfig.DEFAULT_LIMIT;

    @Inject
    public AdminSyslogImpl(Environment environment, Provider<Confirmation> confirmationDialogProvider) {
        super(confirmationDialogProvider, environment.getWidgetStyle());

        syslogFilter = new SyslogFilter(environment.getMessages(), environment.getWidgetStyle()) {
            @Override
            protected void onClearLog() {
                if (presenter != null)
                    presenter.onClearLog();
            }

            @Override
            protected void onContinueLog() {
                if (presenter != null)
                    presenter.onContinueLog();
            }

            @Override
            protected void onPauseLog() {
                if (presenter != null)
                    presenter.onPauseLog();
            }

            @Override
            protected void onLimitChanged(int limit) {
                AdminSyslogImpl.this.limit = limit;
                if (presenter != null)
                    presenter.onFilterLimitChanged(limit);
            }

            @Override
            protected void onLevelChanged(SyslogLevel level) {
                if (presenter != null)
                    presenter.onFilterLevelChanged(level);
            }
        };

        Label emptyLabel = new Label(environment.getMessages().noLogMessagesReceived());
        emptyLabel.addStyleName(environment.getWidgetStyle().FormListEmptyMessage());
        syslogItems = new SyslogItems(emptyLabel);

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        for (SyslogLevel syslogLevel : SyslogLevel.values()) {
            storeLevelListBox.addItem(syslogLevel.name());
        }

        expirationListBox.addItem(managerMessages.minutes());
        expirationListBox.addItem(managerMessages.hours());
        expirationListBox.addItem(managerMessages.days());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        if (presenter == null) {
            storeLevelListBox.setSelectedIndex(SyslogLevel.INFO.ordinal());
            expirationInputText.setValue(null);
            expirationListBox.setSelectedIndex(0);
            syslogFilter.setPaused(false);
            syslogItems.clear();
        }
    }

    @Override
    public void showEvents(SyslogEvent... items) {
        for (SyslogEvent item : items) {
            if (syslogItems.getItemCount() >= limit)
                syslogItems.removeFirstItem();
            syslogItems.addItem(item);
        }
    }

    @Override
    public void clearEvents() {
        syslogItems.clear();
    }

    @Override
    public void setStoredLevel(SyslogLevel level) {
        storeLevelListBox.setSelectedIndex(level.ordinal());
    }

    @Override
    public SyslogLevel geStoredLevel() {
        return SyslogLevel.values()[storeLevelListBox.getSelectedIndex()];
    }

    @Override
    public void setStoredMinutes(int minutes) {
        expirationInputText.setValue(Integer.toString(minutes));
        expirationListBox.setSelectedIndex(0);
    }

    @Override
    public int getStoredMinutes() {
        int enteredValue = expirationInputText.getValue().length() > 0 ? Integer.valueOf(expirationInputText.getValue()) : 0;
        switch (expirationListBox.getSelectedIndex()) {
            case 1:
                return enteredValue * 60;
            case 2:
                return enteredValue * 60 * 24;
        }
        return enteredValue;
    }

    @Override
    public SyslogLevel getFilterLevel() {
        return syslogFilter.getFilterLevel();
    }

    @Override
    public int getFilterLimit() {
        return syslogFilter.getFilterLimit();
    }

    @UiHandler("saveSettingsButton")
    public void onSaveSettingsClicked(ClickEvent event) {
        if (presenter != null)
            presenter.saveSettings();
    }

    @UiHandler("removeAllButton")
    public void onRemoveAllClicked(ClickEvent event) {
        if (presenter != null)
            presenter.removeAll();
    }
}
