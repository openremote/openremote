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
package org.openremote.manager.shared.notification;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BeanParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("notification")
@JsType(isNative = true)
public interface NotificationResource {

    @PUT
    @Path("token")
    @SuccessStatusCode(204)
    @RolesAllowed({"write:user"})
    void storeDeviceToken(@BeanParam RequestParams requestParams,
                          @FormParam("device_id") String deviceId,
                          @FormParam("token") String token);
}
