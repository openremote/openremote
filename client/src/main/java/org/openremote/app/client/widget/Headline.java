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
package org.openremote.app.client.widget;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;

import java.util.logging.Logger;

public class Headline extends FlowPanel {

    private static final Logger LOG = Logger.getLogger(Headline.class.getName());

    protected InlineLabel iconLabel =new InlineLabel();
    protected String icon;
    protected InlineLabel headlineLabel;
    protected Label headlineSubLabel;

    public Headline() {
        this(null);
    }

    public Headline(String text) {
        setStyleName("or-Headline");

        add(iconLabel);
        
        headlineLabel = new InlineLabel();
        headlineLabel.setStyleName("or-HeadlineText");
        add(headlineLabel);

        headlineSubLabel = new Label();
        headlineSubLabel.setStyleName("or-HeadlineSub");
        add(headlineSubLabel);

        setText(text);
    }

    public String getIcon() {
        return icon;
    }

    public void setText(String text) {
        headlineLabel.setText(text);
    }

    public String getText() {
        return headlineLabel.getText();
    }

    public void setSub(String text) {
        headlineSubLabel.setText(text);
        headlineSubLabel.setVisible(text != null && text.length() > 0);
    }

    public String getSub() {
        return headlineSubLabel.getText();
    }

    public void setIcon(String icon) {
        iconLabel.getElement().removeClassName("or-Icon fa");
        iconLabel.getElement().removeClassName("fa-" + this.icon);
        this.icon = icon;
        if (icon != null) {
            iconLabel.getElement().addClassName("or-Icon fa fa-" + icon);
        }
    }
}
