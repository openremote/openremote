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
package org.openremote.manager;

import org.openremote.agent.protocol.AgentModelProvider;
import org.openremote.container.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.util.LogUtil;
import org.openremote.manager.asset.AssetModelService;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.util.ValueUtil;

import com.fasterxml.jackson.databind.JsonNode;

public class Main {

    static {
        LogUtil.initialiseJUL();
    }

    public static void main(String[] args) throws Exception {

        // container.getService(PersistenceService.class);
        // container.getService(AssetModelService.class).initDynamicModel();

        ValueUtil.initialise(null);
        // ValueUtil.doInitialise();
        JsonNode d = ValueUtil.getSchema(AgentLink.class);
        System.out.println(d.toString());
//        Container container = new Container();
//
//        try {
//            container.startBackground();
//        } catch (Exception e) {
//            container.stop();
//            System.exit(1);
//        }
    }
}
