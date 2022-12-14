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
package org.openremote.model.configuration;

import java.io.Serializable;
import java.util.Map;

public class ManagerConf implements Serializable {
    protected boolean loadLocales;
    protected Map<String, String> languages;
    protected Map<String, ManagerConfRealm> realms;
    protected Map<String, Object> pages;
}

class ManagerConfRealm {
    protected String appTitle = null;
    protected String styles = null;
    protected String logo = null;
    protected String logoMobile = null;
    protected String favicon = null;
    protected String language = null;
    protected ManagerHeaders[] headers = null;
}

enum ManagerHeaders {
    rules, insights, gateway, logs, account, users, assets, roles, realms, logout, language, export, map, appearance, provisioning
}
