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
package org.openremote.manager.server.persistence;

import org.openremote.container.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public class ManagerPersistenceService extends PersistenceService {

    @Override
    public void start(Container container) throws Exception {

        // Before schema is generated, execute this
        persistenceUnitProperties.put("javax.persistence.schema-generation.create-source", "script-then-metadata");
        persistenceUnitProperties.put("javax.persistence.schema-generation.create-script-source", "Extensions.sql");

        // After schema is generated, execute this
        List<String> importFiles = new ArrayList<>();
        if (!container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {
            importFiles.add("BasicIdentityProvider.sql");
        }
        importFiles.add("GetAssetTreePath.sql");
        importFiles.add("Constraints.sql");
        persistenceUnitProperties.put(
            "hibernate.hbm2ddl.import_files",
            TextUtil.toCommaSeparated(importFiles.toArray(new String[importFiles.size()]))
        );

        super.start(container);
    }
}
