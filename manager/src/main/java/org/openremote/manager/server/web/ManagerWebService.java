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
package org.openremote.manager.server.web;

import org.openremote.container.Container;
import org.openremote.container.web.WebService;

import java.nio.file.Paths;

import static org.openremote.manager.server.Constants.MASTER_REALM;

public class ManagerWebService extends WebService {

    public static final String WEBSERVER_DOCROOT = "WEBSERVER_DOCROOT";
    public static final String WEBSERVER_DOCROOT_DEFAULT = "src/main/webapp";

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        setDefaultRealm(MASTER_REALM);
        setStaticResourceDocRoot(Paths.get(container.getConfig(WEBSERVER_DOCROOT, WEBSERVER_DOCROOT_DEFAULT)));
    }
}
