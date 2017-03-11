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
package org.openremote.manager.server.setup;

import com.vividsolutions.jts.geom.GeometryFactory;
import org.openremote.container.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.rules.RulesStorageService;

public abstract class AbstractManagerSetup implements Setup {

    final protected PersistenceService persistenceService;
    final protected AssetStorageService assetStorageService;
    final protected RulesStorageService rulesStorageService;
    final protected GeometryFactory geometryFactory = new GeometryFactory();
    final protected SetupService setupService;

    public AbstractManagerSetup(Container container) {
        this.persistenceService = container.getService(PersistenceService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.rulesStorageService = container.getService(RulesStorageService.class);
        this.setupService = container.getService(SetupService.class);
    }

}
