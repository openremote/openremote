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
package org.openremote.manager.client;

public class ThemeStyle {

    public static final String PREFIX = "theme-";

    public String HeaderPanel() {
        return PREFIX + "HeaderPanel";
    }

    public String HeaderHorizontalSeparator() {
        return PREFIX + "HeaderHorizontalSeparator";
    }

    public String PopupPanel() {
        return PREFIX + "PopupPanel";
    }

    public String NavItemMain() {
        return PREFIX + "NavItemMain";
    }

    public String NavItem() {
        return PREFIX + "NavItem";
    }

    public String NavItemIconOnly() {
        return PREFIX + "NavItemIconOnly";
    }

    public String SidePanel() {
        return PREFIX + "SidePanel";
    }

}