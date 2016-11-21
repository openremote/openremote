/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.controllercommand.resources;

import flexjson.JSONSerializer;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:eric@openremote.org">Eric Bariaux</a>
 */
@Path("/")
public class ControllerCommandsResource {
    @Inject
    private ControllerCommandService controllerCommandService;

    protected final static Logger log = LoggerFactory.getLogger(ControllerCommandsResource.class);

    /**
     * Return a list of all not finished ControllerCommands<p>
     * REST Url: /rest/commands/{controllerOid} -> return all not finished controller commands for the given controllerOid
     *
     * @return a List of ControllerCommand
     */

    @GET
    @Path("commands/{controllerOid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadControllerCommands(@Context HttpServletRequest request, @PathParam("controllerOid") String controllerOid) {
        GenericResourceResultWithErrorMessage result = null;
        try {
            if (controllerOid != null) {
                log.info("Asked to get controller commands list for controller id " + controllerOid);
                Long id = Long.valueOf(controllerOid);

                String username = request.getUserPrincipal().getName();
                log.info("Query done by " + username);

                List<ControllerCommandDTO> commands = controllerCommandService.queryByControllerOidForUser(getEntityManager(request), id, username);
                result = new GenericResourceResultWithErrorMessage(null, commands);
            } else {
                log.info("No controller oid provided");
                result = new GenericResourceResultWithErrorMessage(null, new ArrayList<ControllerCommandDTO>());
            }
        } catch (Exception e) {
            log.error("Error getting controller commands", e);
            result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
        }
        return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
    }

    private EntityManager getEntityManager(HttpServletRequest request) {
        return (EntityManager) request.getAttribute(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
    }

}
