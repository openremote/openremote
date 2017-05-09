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
package org.openremote.manager.client.admin.syslog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.*;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;

import javax.inject.Inject;
import java.util.Arrays;

public class QuickSyslogImpl extends AbstractAppPanel implements QuickSyslog {

    interface UI extends UiBinder<PopupPanel, QuickSyslogImpl> {
    }

    interface Style extends CssResource {
        String popup();

        String filterFormGroup();

        String logItemLevelCategory();

        String header();

        String logPanel();

        String filterForm();

        String panel();

        String filterFormLabel();

        String filterFormField();

        String logItem();
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    QuickSyslogImpl.Style style;

    @UiField
    FlowPanel logPanel;

    @UiField
    FormValueListBox<SyslogLevel> levelListBox;

    @UiField
    PushButton pauseButton;
    @UiField
    PushButton continueButton;
    @UiField
    PushButton clearButton;

    Presenter presenter;

    boolean empty;
    boolean paused;

    @Inject
    public QuickSyslogImpl() {
        super(GWT.create(UI.class));

        setOpenCloseConsumer(isOpen -> {
            if (presenter == null)
                return;
            if (isOpen) {
                presenter.onOpen();
            } else {
                resetLogPanel();
                presenter.onClose();
            }
        });

        levelListBox.setValue(SyslogLevel.INFO);
        levelListBox.setAcceptableValues(Arrays.asList(SyslogLevel.values()));
        levelListBox.addValueChangeHandler(event -> {
            if (presenter != null) {
                presenter.onLogLevelChanged(event.getValue());
            }
        });

        pauseButton.addClickHandler(event -> pause(true));
        continueButton.addClickHandler(event -> pause(false));

        resetLogPanel();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public SyslogLevel getLogLevel() {
        return levelListBox.getValue();
    }

    @Override
    public void addEvent(SyslogEvent event) {
        if (paused)
            return;

        // Remove empty message label
        if (empty)
            logPanel.remove(0);
        empty = false;

        // Limit to 200 messages
        if (logPanel.getWidgetCount() >= 200)
            logPanel.remove(0);

        logPanel.add(createItem(event));

        scrollToBottom();
    }

    @UiHandler("clearButton")
    public void clearClicked(ClickEvent event) {
        resetLogPanel();
    }

    protected void resetLogPanel() {
        logPanel.clear();
        empty = true;
        Label emptyMessage = new Label(managerMessages.noLogMessagesReceived());
        emptyMessage.addStyleName(widgetStyle.FormListEmptyMessage());
        logPanel.add(emptyMessage);
    }

    protected Widget createItem(SyslogEvent event) {
        FlowPanel itemPanel = new FlowPanel();
        itemPanel.addStyleName(style.logItem());
        itemPanel.addStyleName(widgetStyle.FormListItem());

        FlowPanel levelCategoryPanel = new FlowPanel();
        levelCategoryPanel.setStyleName("layout horizontal center");
        levelCategoryPanel.addStyleName(style.logItemLevelCategory());

        MessagesIcon levelIcon = new MessagesIcon(event.getLevel().getIcon());
        levelCategoryPanel.add(levelIcon);

        Label categoryLabel = new Label();
        categoryLabel.setText(event.getCategory().name());
        levelCategoryPanel.add(categoryLabel);

        event.getSubCategoryOptional().ifPresent(s -> {
            Label subCategoryLabel= new Label();
            subCategoryLabel.setText(": " + s);
            levelCategoryPanel.add(subCategoryLabel);
        });

        itemPanel.add(levelCategoryPanel);

        Label messageLabel = new Label();
        messageLabel.setText(event.getMessage());
        itemPanel.add(messageLabel);

        return itemPanel;
    }

    protected void scrollToBottom() {
        logPanel.getElement().setScrollTop(logPanel.getElement().getScrollHeight());
    }

    protected void pause(boolean pause) {
        this.paused = pause;
        pauseButton.setVisible(!pause);
        continueButton.setVisible(pause);
        if (!pause)
            scrollToBottom();
    }
}