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

import org.keycloak.theme.FolderThemeProvider;
import org.keycloak.theme.Theme;

import java.io.File;
import java.io.IOException;

/**
 * This theme provider will fallback to the openremote theme in the fallback dir if the custom theme cannot be found
 */
public class CustomThemeProvider extends FolderThemeProvider {

    FolderThemeProvider fallbackProvider = null;

    public CustomThemeProvider(File themesDir) {
        super(themesDir);
    }

    @Override
    public int getProviderPriority() {
        return super.getProviderPriority() - 10;
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) throws IOException {
        if (!super.hasTheme(name, type) && fallbackProvider != null) {
            return fallbackProvider.getTheme("openremote", type);
        }

        return super.getTheme(name, type);
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        if (!super.hasTheme(name, type) && fallbackProvider != null) {
            return fallbackProvider.hasTheme("openremote", type);
        }

        return true;
    }
}
