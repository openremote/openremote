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
package org.openremote.model.console;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.asset.impl.ConsoleAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.http.RequestParams;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Console")
@Path("console")
public interface ConsoleResource {

    /**
     * Creates or updates the registration for the specified console; if the {@link ConsoleRegistration#getId} contains
     * an ID then it is an update operation otherwise it is a create operation, in both cases the saved {@link
     * ConsoleRegistration} is returned which should be used for future calls to this endpoint.
     * <p>
     * Behind the scenes the console registration is converted into an asset and the {@link ConsoleRegistration} data is
     * stored in the appropriate {@link ConsoleAsset} {@link Attribute}s.
     * <p>
     * This is a public endpoint and allows the registration of consoles anonymously; if there is an authenticated user
     * registering the console  then the console asset will be linked to that user. If multiple users login on the same
     * console then it will be associated with each user (i.e. a 1-many relationship).
     */
    @POST
    @Path("register")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    ConsoleRegistration register(@BeanParam RequestParams requestParams, @NotNull @Valid ConsoleRegistration consoleRegistration);
}
