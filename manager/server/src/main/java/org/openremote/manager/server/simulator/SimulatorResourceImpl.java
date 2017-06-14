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
package org.openremote.manager.server.simulator;

import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.simulator.SimulatorResource;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.attribute.AttributeRef;

public class SimulatorResourceImpl extends ManagerWebResource implements SimulatorResource {

    final protected SimulatorProtocol simulatorProtocol;
    final protected AssetStorageService assetStorageService;

    public SimulatorResourceImpl(ManagerIdentityService identityService,
                                 SimulatorProtocol simulatorProtocol,
                                 AssetStorageService assetStorageService) {
        super(identityService);
        this.simulatorProtocol = simulatorProtocol;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public SimulatorState getSimulatorState(RequestParams requestParams, String agentId, String protocolConfiguration) {
        SimulatorState simulatorState =
            simulatorProtocol.getSimulatorState(new AttributeRef(agentId, protocolConfiguration)).orElse(null);

        simulatorState.updateAttributeDetails(
            assetStorageService.findNames()
        );


        return simulatorState;
    }

    @Override
    public void updateSimulatorState(RequestParams requestParams, AttributeRef protocolConfigurationRef, SimulatorState simulatorState) {

    }
}
