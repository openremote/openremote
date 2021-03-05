/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FolderThemeProvider;
import org.keycloak.theme.ThemeProvider;
import org.keycloak.theme.ThemeProviderFactory;

import java.io.File;

/**
 * A theme provider to load custom themes from the deployment directory, the openremote theme is baked
 * into the standard theme directory.
 */
public class CustomThemeProviderFactory implements ThemeProviderFactory {

    protected CustomThemeProvider themeProvider;

    @Override
    public ThemeProvider create(KeycloakSession session) {
        if (themeProvider.fallbackProvider == null) {
            themeProvider.fallbackProvider = (FolderThemeProvider) session.getProvider(ThemeProvider.class, "folder");
        }
        return themeProvider;
    }

    @Override
    public void init(Config.Scope config) {
        File rootDir = new File("/deployment/keycloak/themes");
        themeProvider = new CustomThemeProvider(rootDir);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "openremote-custom-folder";
    }
}
