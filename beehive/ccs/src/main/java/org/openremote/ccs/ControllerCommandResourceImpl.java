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
package org.openremote.ccs;

import flexjson.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.ccs.model.Account;
import org.openremote.ccs.model.ControllerCommand;
import org.openremote.ccs.model.User;
import org.openremote.container.web.WebResource;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.ControllerCommandDTO.Type;
import org.openremote.rest.GenericResourceResultWithErrorMessage;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControllerCommandResourceImpl extends WebResource implements ControllerCommandResource {

    private static final Logger LOG = Logger.getLogger(ControllerCommandResourceImpl.class.getName());

    final protected CCSPersistenceService persistenceService;

    public ControllerCommandResourceImpl(CCSPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public Response saveCommand(@Context HttpServletRequest request, String jsonString) {
        if (jsonString == null) {
            throw new BadRequestException("No data received");
        }

        String username = request.getUserPrincipal().getName();

        return persistenceService.doReturningTransaction(entityManager -> {
            try {
                User user = persistenceService.loadByUsername(entityManager, username);
                Account account = user.getAccount();

                JSONObject jsonData = new JSONObject(jsonString);
                String typeAsString = jsonData.getString("type");
                if (typeAsString == null) {
                    throw new BadRequestException("Type must be provided");
                }
                try {
                    ControllerCommandDTO.Type type = Type.valueOf(typeAsString.trim().toUpperCase());
                    if (Type.DOWNLOAD_DESIGN != type) {
                        throw new BadRequestException("Unsupported command type");
                    }
                    ControllerCommand command = new ControllerCommand(account, Type.DOWNLOAD_DESIGN);
                    persistenceService.save(entityManager, command);
                    GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(null, command);
                    return Response.ok(new JSONSerializer().exclude("*.class").exclude("result.account").deepSerialize(result)).build();
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Unknown command type");
                }

            } catch (JSONException e) {
                throw new BadRequestException("Invalid JSON payload");
            }
        });
    }

    @Override
    public Response ackControllerCommands(@Context HttpServletRequest request, String commandId) {
        GenericResourceResultWithErrorMessage result = null;
        try {
            if (commandId != null) {
                LOG.info("Asked to acknowledge command with id " + commandId);
                Long id = Long.valueOf(commandId);
                persistenceService.doTransaction(entityManager -> {
                    ControllerCommand controllerCommand = entityManager.find(ControllerCommand.class, id);
                    controllerCommand.setState(ControllerCommand.State.DONE);
                    controllerCommand = entityManager.merge(controllerCommand);
                });
                result = new GenericResourceResultWithErrorMessage(null, "ok");
            } else {
                result = new GenericResourceResultWithErrorMessage("command not found", null);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error while acknowledging a ControllerCommand", ex);
            result = new GenericResourceResultWithErrorMessage(ex.getMessage(), null);
        }
        return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
    }

    @Override
    public Response loadControllerCommands(@Context HttpServletRequest request, String controllerId) {
        GenericResourceResultWithErrorMessage result = null;
        try {
            if (controllerId != null) {
                LOG.info("Asked to get controller commands list for controller id " + controllerId);
                Long id = Long.valueOf(controllerId);

                String username = request.getUserPrincipal().getName();
                LOG.info("Query done by " + username);

                result = persistenceService.doReturningTransaction(entityManager -> {
                    List<ControllerCommandDTO> commands = persistenceService.queryByControllerOidForUser(entityManager, id, username);
                    return new GenericResourceResultWithErrorMessage(null, commands);
                });
            } else {
                LOG.info("No controller oid provided");
                result = new GenericResourceResultWithErrorMessage(null, new ArrayList<ControllerCommandDTO>());
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error getting controller commands", ex);
            result = new GenericResourceResultWithErrorMessage(ex.getMessage(), null);
        }
        return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
    }
}
