/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.manager;

public class ManagerAppRealmConfig {
    protected String appTitle;
    protected String styles;
    protected String logo;
    protected String logoMobile;
    protected String favicon;
    protected String language;
    protected String[] headers;
    protected ManagerAppRealmNotificationConfig notifications;

    public String getAppTitle() {
        return appTitle;
    }

    public ManagerAppRealmConfig setAppTitle(String appTitle) {
        this.appTitle = appTitle;
        return this;
    }

    public String getStyles() {
        return styles;
    }

    public ManagerAppRealmConfig setStyles(String styles) {
        this.styles = styles;
        return this;
    }

    public String getLogo() {
        return logo;
    }

    public ManagerAppRealmConfig setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public String getLogoMobile() {
        return logoMobile;
    }

    public ManagerAppRealmConfig setLogoMobile(String logoMobile) {
        this.logoMobile = logoMobile;
        return this;
    }

    public String getFavicon() {
        return favicon;
    }

    public ManagerAppRealmConfig setFavicon(String favicon) {
        this.favicon = favicon;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public ManagerAppRealmConfig setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String[] getHeaders() {
        return headers;
    }

    public ManagerAppRealmConfig setHeaders(String[] headers) {
        this.headers = headers;
        return this;
    }

    public ManagerAppRealmNotificationConfig getNotifications() {
        return notifications;
    }

    public ManagerAppRealmConfig setNotifications(ManagerAppRealmNotificationConfig notifications) {
        this.notifications = notifications;
        return this;
    }
}
