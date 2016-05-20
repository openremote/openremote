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
package org.openremote.manager.client.style;

public abstract class AbstractStyle {
    
    abstract String getPrefix();

    public String Header() {
        return getPrefix() + "Header";
    }

    public String Footer() {
        return getPrefix() + "Footer";
    }

    public String NavItem() {
        return getPrefix() + "NavItem";
    }

    public String NavItemActive() {
        return getPrefix() + "NavItemActive";
    }

    public String SidebarContent() {
        return getPrefix() + "SidebarContent";
    }

    public String MainContent() {
        return getPrefix() + "MainContent";
    }

    public String PushButton() {
        return getPrefix() + "PushButton";
    }

    public String Hyperlink() {
        return getPrefix() + "Hyperlink";
    }

    public String UnorderedList() {
        return getPrefix() + "UnorderedList";
    }

    public String Toast() {
        return getPrefix() + "Toast";
    }

    public String ToastInfo() {
        return getPrefix() + "ToastInfo";
    }

    public String ToastFailure() {
        return getPrefix() + "ToastFailure";
    }

    public String MessagesIcon() {
        return getPrefix() + "MessagesIcon";
    }

    public String PopupPanel() {
        return getPrefix() + "PopupPanel";
    }

    public String PopupPanelHeader() {
        return getPrefix() + "PopupPanelHeader";
    }

    public String PopupPanelContent() {
        return getPrefix() + "PopupPanelContent";
    }

    public String PopupPanelFooter() {
        return getPrefix() + "PopupPanelFooter";
    }

    public String Headline1() {
        return getPrefix() + "Headline1";
    }
}
