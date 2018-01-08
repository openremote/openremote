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
package org.openremote.model.apps;

import jsinterop.annotations.JsType;
import org.openremote.model.security.Tenant;

/**
 * An installed console app.
 */
@JsType
public class ConsoleApp {

    public Tenant tenant;
    public String url;

    protected ConsoleApp() {
        this(null, null);
    }

    public ConsoleApp(Tenant tenant, String url) {
        setTenant(tenant);
        setUrl(url);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "tenant='" + tenant + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}
