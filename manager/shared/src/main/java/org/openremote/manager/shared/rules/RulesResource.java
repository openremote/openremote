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
package org.openremote.manager.shared.rules;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("rules")
@JsType(isNative = true)
public interface RulesResource {

    /**
     * Retrieve global rules. Only the superuser can perform this operation, a 403 status is
     * returned if a regular user tries to access global rule definitions.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    GlobalRulesDefinition[] getGlobalDefinitions(@BeanParam RequestParams requestParams);

    /**
     * Retrieve rules of a tenant. The superuser can retrieve rules of all realms, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm.
     * An empty result will be returned if the realm can not be found.
     */
    @GET
    @Path("tenant/for/{realm}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    TenantRulesDefinition[] getTenantDefinitions(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * Retrieve rules of an asset. The superuser can retrieve rules of all realms and assets, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm, or
     * if the user is restricted and the asset is not one of its linked assets. An empty result will be returned
     * if the asset can not be found.
     */
    @GET
    @Path("asset/for/{assetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    AssetRulesDefinition[] getAssetDefinitions(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /* ################################################################################################# */

    /**
     * Create a global rules definition. Only the superuser can perform this operation, a 403 status is
     * returned if a regular user tries to access global rule definitions.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void createGlobalDefinition(@BeanParam RequestParams requestParams, GlobalRulesDefinition rulesDefinition);

    /**
     * Retrieve a global rules definition. Only the superuser can perform this operation, a 403 status is
     * returned if a regular user tries to access global rule definitions.
     */
    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    GlobalRulesDefinition getGlobalDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a global rules definition. Only the superuser can perform this operation, a 403 status is
     * returned if a regular user tries to access global rule definitions.
     */
    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void updateGlobalDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id, GlobalRulesDefinition rulesDefinition);

    /**
     * Deletes a global rules definition. Only the superuser can perform this operation, a 403 status is
     * returned if a regular user tries to access global rule definitions.
     */
    @DELETE
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void deleteGlobalDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create a tenant rules definition. The superuser can create rules in all realms, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted.
     */
    @POST
    @Path("tenant")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void createTenantDefinition(@BeanParam RequestParams requestParams, TenantRulesDefinition rulesDefinition);

    /**
     * Retrieve a tenant rules definition. The superuser can retrieve rules of all realms, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm, or
     * if the user is restricted.
     */
    @GET
    @Path("tenant/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    TenantRulesDefinition getTenantDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a tenant rules definition. The superuser can update rules of all realms, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted.
     */
    @PUT
    @Path("tenant/{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void updateTenantDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id, TenantRulesDefinition rulesDefinition);

    /**
     * Delete a tenant rules definition. The superuser can delete rules of all realms, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted.
     */
    @DELETE
    @Path("tenant/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void deleteTenantDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create an asset rules definition. The superuser can create rules for all assets, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted and does not have access to the asset.
     */
    @POST
    @Path("asset")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void createAssetDefinition(@BeanParam RequestParams requestParams, AssetRulesDefinition rulesDefinition);

    /**
     * Retrieve an asset rules definition. The superuser can retrieve rules of all assets, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm, or
     * if the user is restricted and does not have access to the asset.
     */
    @GET
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"write:rules"})
    AssetRulesDefinition getAssetDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update an asset rules definition. The superuser can update rules of all assets, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted and does not have access to the asset.
     */
    @PUT
    @Path("asset/{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void updateAssetDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id, AssetRulesDefinition rulesDefinition);

    /**
     * Delete an asset rules definition. The superuser can delete rules of all assets, a 403 status
     * is returned if a regular user tries to access rule definitions outside of its authenticated realm,
     * or if the user is restricted and does not have access to the asset.
     */
    @DELETE
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    void deleteAssetDefinition(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

}
