/*
 * Copyright 2022, OpenRemote Inc.
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

import org.openremote.model.auth.UsernamePassword;

public class ManagerConfig {
    protected String managerUrl;
    protected String keycloakUrl;
    protected String appVersion;
    protected String realm;
    protected String clientId;
    protected boolean autoLogin;
    protected boolean consoleAutoEnable;
    protected boolean loadIcons;
    protected int pollingIntervalMillis;
    protected String[] loadTranslations;
    protected boolean loadDescriptors;
    protected String translationsLoadPath;
    protected boolean skipFallbackToBasicAuth;
    protected boolean applyConfigToAdmin;
    protected Auth auth;
    protected UsernamePassword credentials;
    protected EventProviderType eventProviderType;
    protected MapType mapType;
    protected Object configureTranslationsOptions;
    protected Object basicLoginProvider;

}
