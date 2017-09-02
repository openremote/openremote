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

import com.google.gwt.user.client.ui.FlowPanel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.*;
import org.openremote.model.syslog.SyslogLevel;

import static org.openremote.manager.shared.syslog.SyslogConfig.DEFAULT_LEVEL;
import static org.openremote.manager.shared.syslog.SyslogConfig.DEFAULT_LIMIT;

public abstract class SyslogFilter extends FlowPanel {

    final protected FormGroup formGroup;
    final protected FormListBox limitListBox;
    final protected FormListBox levelListBox;
    final protected FormButton pauseButton;
    final protected FormButton continueButton;

    public SyslogFilter(ManagerMessages messages, WidgetStyle widgetStyle) {
        addStyleName("flex-none layout vertical");
        addStyleName(widgetStyle.MainContent());

        formGroup = new FormGroup();
        add(formGroup);

        FormLabel showLastLabel = new FormLabel(messages.showLast());
        showLastLabel.addStyleName("inline");
        formGroup.setFormLabel(showLastLabel);

        FormField field = new FormField();
        formGroup.setFormField(field);
        FlowPanel fieldPanel = new FlowPanel();
        field.add(fieldPanel);

        limitListBox = new FormListBox();
        fieldPanel.add(limitListBox);
        limitListBox.addItem("15");
        limitListBox.addItem("50");
        limitListBox.addItem("200");
        limitListBox.addChangeHandler(event -> onLimitChanged(getFilterLimit()));

        levelListBox = new FormListBox();
        fieldPanel.add(levelListBox);
        for (SyslogLevel syslogLevel : SyslogLevel.values()) {
            levelListBox.addItem(syslogLevel.name());
        }
        levelListBox.addChangeHandler(event -> onLevelChanged(getFilterLevel()));
        FormLabel eventsLabel = new FormLabel(messages.events());
        eventsLabel.addStyleName("inline");
        fieldPanel.add(eventsLabel);

        FormGroupActions actions = new FormGroupActions();
        formGroup.setFromGroupActions(actions);

        pauseButton = new FormButton(messages.pauseLog());
        pauseButton.setIcon("pause");
        actions.add(pauseButton);
        continueButton = new FormButton(messages.continueLog());
        continueButton.setIcon("play");
        actions.add(continueButton);

        pauseButton.addClickHandler(event -> {
            pauseButton.setVisible(false);
            continueButton.setVisible(true);
            onPauseLog();
        });

        continueButton.addClickHandler(event -> {
            pauseButton.setVisible(true);
            continueButton.setVisible(false);
            onContinueLog();
        });

        FormButton clearButton = new FormButton(messages.clear());
        clearButton.setIcon("eraser");
        clearButton.addClickHandler(event -> onClearLog());
        actions.add(clearButton);

        switch (DEFAULT_LIMIT) {
            case 15:
                limitListBox.setSelectedIndex(0);
                break;
            case 50:
                limitListBox.setSelectedIndex(1);
                break;
            default:
                limitListBox.setSelectedIndex(2);
                break;
        }
        levelListBox.setSelectedIndex(DEFAULT_LEVEL.ordinal());
        setPaused(false);
    }

    public SyslogLevel getFilterLevel() {
        return SyslogLevel.valueOf(levelListBox.getSelectedValue());
    }

    public int getFilterLimit() {
        return Integer.valueOf(limitListBox.getSelectedValue());
    }

    public void setPaused(boolean paused) {
        pauseButton.setVisible(!paused);
        continueButton.setVisible(paused);
    }

    protected abstract void onClearLog();

    protected abstract void onContinueLog();

    protected abstract void onPauseLog();

    protected abstract void onLimitChanged(int limit);

    protected abstract void onLevelChanged(SyslogLevel level);

}
