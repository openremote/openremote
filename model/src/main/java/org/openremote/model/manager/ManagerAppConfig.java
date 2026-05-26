/*
 * Copyright 2022, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.manager;

import java.io.Serializable;
import java.util.Map;

public class ManagerAppConfig implements Serializable {
  protected boolean loadLocales;
  protected Map<String, String> languages;
  protected Map<String, ManagerAppRealmConfig> realms;
  protected Map<String, Object> pages;
  protected ManagerConfig manager;

  public boolean isLoadLocales() {
    return loadLocales;
  }

  public ManagerAppConfig setLoadLocales(boolean loadLocales) {
    this.loadLocales = loadLocales;
    return this;
  }

  public Map<String, String> getLanguages() {
    return languages;
  }

  public ManagerAppConfig setLanguages(Map<String, String> languages) {
    this.languages = languages;
    return this;
  }

  public Map<String, ManagerAppRealmConfig> getRealms() {
    return realms;
  }

  public ManagerAppConfig setRealms(Map<String, ManagerAppRealmConfig> realms) {
    this.realms = realms;
    return this;
  }

  public Map<String, Object> getPages() {
    return pages;
  }

  public ManagerAppConfig setPages(Map<String, Object> pages) {
    this.pages = pages;
    return this;
  }

  public ManagerConfig getManager() {
    return manager;
  }

  public ManagerAppConfig setManager(ManagerConfig manager) {
    this.manager = manager;
    return this;
  }
}
