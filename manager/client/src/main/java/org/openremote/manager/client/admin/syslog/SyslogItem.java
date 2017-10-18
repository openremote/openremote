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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import org.openremote.model.Constants;
import org.openremote.model.syslog.SyslogEvent;

import java.util.Date;

/**
 * Lightweight, not a Widget.
 */
public class SyslogItem {

    final protected Element element;

    public interface Templates extends SafeHtmlTemplates {
        @Template(
            "<div class=\"layout horizontal center\" style=\"font-weight: 600; margin-bottom: 0.2em;\">" +
                "<span class=\"or-IconLabel or-MessagesIcon fa fa-{0} {1}\"></span>" +
                "<span style=\"font-size:smaller; margin-left: 1em;\">{2}</span>" +
                "<span class=\"flex layout horizontal end-justified\" style=\"font-size:smaller; margin-left: 1em;\">{3}</span>" +
                "</div>" +
                "<div style=\"font-size:smaller;white-space:pre-line;\">{4}</div>"
        )
        SafeHtml eventTemplate(String icon, String level, String category, String date, String message);
    }

    private static final Templates TEMPLATES = GWT.create(Templates.class);


    public SyslogItem(SyslogEvent event) {
        element = Document.get().createElement(DivElement.TAG);
        element.setClassName("flex-none layout vertical or-FormListItem");
        String level;
        switch (event.getLevel()) {
            case WARN:
                level = "warn";
                break;
            case ERROR:
                level = "error";
                break;
            default:
                level = "";
        }
        element.setInnerSafeHtml(TEMPLATES.eventTemplate(
            event.getLevel().getIcon(),
            level,
            event.getCategoryLabel(),
            DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT_MILLIS).format(new Date(event.getTimestamp())),
            event.getMessage()
        ));
    }

    public Element getElement() {
        return element;
    }
}
