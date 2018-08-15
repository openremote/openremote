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
package org.openremote.model.notification;

import jsinterop.annotations.JsType;
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;
import org.openremote.model.value.Value;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("notification")
@JsType(isNative = true)
public interface NotificationResource {

    /**
     * Gets all sent notifications that have been sent to the specified targets; optionally limiting the scope of the
     * request by {@link AbstractNotificationMessage} type and/or sent datetime. If type(s) or timestamp are not set
     * then it is assumed no type or time constraint is required. Can also provide a list of notification IDs to get
     * specific notifications.
     * <p>
     * Only the superuser can call this operation.
     */
    @GET
    @SuccessStatusCode(200)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"read:admin"})
    @SuppressWarnings("unusable-by-js")
    SentNotification[] getNotifications(@BeanParam RequestParams requestParams,
                                        @QueryParam("ids") List<Long> ids,
                                        @QueryParam("types") List<String> types,
                                        @QueryParam("before") Long timestamp,
                                        @QueryParam("tenantId") List<String> tenantIds,
                                        @QueryParam("userId") List<String> userIds,
                                        @QueryParam("assetId") List<String> assetIds);

    /**
     * Removes all sent notifications that have been sent to the specified targets; optionally limiting the scope of the
     * request by {@link AbstractNotificationMessage} type and/or sent datetime. If type(s) or timestamp are not set
     * then it is assumed no type or time constraint is required. Can also provide a list of notification IDs to delete
     * specific notifications.
     * <p>
     * Only the superuser can call this operation.
     */
    @DELETE
    @SuccessStatusCode(204)
    @RolesAllowed({"write:admin"})
    @SuppressWarnings("unusable-by-js")
    void removeNotifications(@BeanParam RequestParams requestParams,
                             @QueryParam("ids") List<Long> ids,
                             @QueryParam("types") List<String> types,
                             @QueryParam("before") Long timestamp,
                             @QueryParam("tenantIds") List<String> tenantIds,
                             @QueryParam("userIds") List<String> userIds,
                             @QueryParam("assetIds") List<String> assetIds);

    /**
     * Remove a specific sent notification by ID.
     * <p>
     * Only the superuser can call this operation.
     */
    @DELETE
    @Path("{notificationId}")
    @SuccessStatusCode(204)
    @RolesAllowed({"write:admin"})
    @SuppressWarnings("unusable-by-js")
    void removeNotification(@BeanParam RequestParams requestParams,
                            @PathParam("notificationId") Long notificationId);

    /**
     * Send a notification to one or more targets; the authorisation of the requesting user will determine whether or
     * not the targets can be contacted; if one or more targets are not accessible due to permissions then the entire
     * request will fail with a 403 response.
     */
    @POST
    @Path("alert")
    @SuccessStatusCode(204)
    @Consumes(APPLICATION_JSON)
    @SuppressWarnings("unusable-by-js")
    void sendNotification(@BeanParam RequestParams requestParams,
                          Notification notification);

    /**
     * Allows a target to mark a notification as delivered.
     * <p>
     * The requesting user must have permission to acknowledge the specified notification otherwise a 403 response
     * is returned.
     */
    @PUT
    @Path("{notificationId}/delivered")
    @SuccessStatusCode(204)
    @SuppressWarnings("unusable-by-js")
    void notificationDelivered(@BeanParam RequestParams requestParams,
                               @QueryParam("targetId") String targetId,
                               @PathParam("notificationId") Long notificationId);

    /**
     * Allows a target to acknowledge a notification with an optional acknowledgement value.
     * <p>
     * The requesting user must have permission to acknowledge the specified notification otherwise a 403 response
     * is returned.
     */
    @PUT
    @Path("{notificationId}/acknowledged")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @SuppressWarnings("unusable-by-js")
    void notificationAcknowledged(@BeanParam RequestParams requestParams,
                                  @QueryParam("targetId") String targetId,
                                  @PathParam("notificationId") Long notificationId,
                                  Value acknowledgement);
}
