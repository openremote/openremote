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
package org.openremote.model.rules;

import jsinterop.annotations.JsType;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeDescriptorImpl;
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;
import org.openremote.model.rules.geofence.GeofenceDefinition;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("rules")
@JsType(isNative = true)
public interface RulesResource {

    /**
     * Retrieve global rules. Only the superuser can perform this operation, a 403 status is returned if a regular user
     * tries to access global rulesets.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    @SuppressWarnings("unusable-by-js")
    GlobalRuleset[] getGlobalRulesets(@BeanParam RequestParams requestParams);

    /**
     * Retrieve rules of a tenant. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access rulesets outside of its authenticated realm. An empty result will be returned if the realm
     * can not be found.
     */
    @GET
    @Path("tenant/for/{realmId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    @SuppressWarnings("unusable-by-js")
    TenantRuleset[] getTenantRulesets(@BeanParam RequestParams requestParams, @PathParam("realmId") String realmId);

    /**
     * Retrieve rules of an asset. The superuser can retrieve rules of all realms and assets, a 403 status is returned
     * if a regular user tries to access ruleset outside of its authenticated realm, or if the user is restricted and
     * the asset is not one of its linked assets. An empty result will be returned if the asset can not be found.
     */
    @GET
    @Path("asset/for/{assetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    @SuppressWarnings("unusable-by-js")
    AssetRuleset[] getAssetRulesets(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /* ################################################################################################# */

    /**
     * Create a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void createGlobalRuleset(@BeanParam RequestParams requestParams, @Valid GlobalRuleset ruleset);

    /**
     * Retrieve a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    @SuppressWarnings("unusable-by-js")
    GlobalRuleset getGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a global rules ruleset. Only the superuser can perform this operation, a 403 status is returned if a
     * regular user tries to access global ruleset.
     */
    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void updateGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid GlobalRuleset ruleset);

    /**
     * Deletes a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @DELETE
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void deleteGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create a tenant ruleset. The superuser can create rules in all realms, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @POST
    @Path("tenant")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void createTenantRuleset(@BeanParam RequestParams requestParams, @Valid TenantRuleset ruleset);

    /**
     * Retrieve a tenant ruleset. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @GET
    @Path("tenant/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:rules"})
    @SuppressWarnings("unusable-by-js")
    TenantRuleset getTenantRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a tenant ruleset. The superuser can update rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @PUT
    @Path("tenant/{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void updateTenantRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid TenantRuleset ruleset);

    /**
     * Delete a tenant ruleset. The superuser can delete rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @DELETE
    @Path("tenant/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void deleteTenantRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create an asset ruleset. The superuser can create rules for all assets, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have
     * access to the asset.
     */
    @POST
    @Path("asset")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void createAssetRuleset(@BeanParam RequestParams requestParams, @Valid AssetRuleset ruleset);

    /**
     * Retrieve an asset ruleset. The superuser can retrieve rules of all assets, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have
     * access to the asset.
     */
    @GET
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    AssetRuleset getAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update an asset ruleset. The superuser can update rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @PUT
    @Path("asset/{id}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void updateAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid AssetRuleset ruleset);

    /**
     * Delete an asset ruleset. The superuser can delete rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @DELETE
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:rules"})
    @SuppressWarnings("unusable-by-js")
    void deleteAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);


    /**
     * Get the geofences for the specified asset; if this method is accessed anonymously (public read) then the asset
     * must have public read enabled and the {@link AttributeDescriptorImpl#CONSOLE_PROVIDER_GEOFENCE} attribute must
     * have a {@link AssetMeta#ACCESS_PUBLIC_READ} meta item. Otherwise the asset must be linked to the logged in user.
     * If neither of these conditions are met then a 403 is returned.
     * <p>
     * The attribute must also have a valid {@link AssetMeta#GEOFENCE_ADAPTER}.
     */
    @GET
    @Path("geofences/{assetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    GeofenceDefinition[] getAssetGeofences(@PathParam("assetId") String assetId);
}
