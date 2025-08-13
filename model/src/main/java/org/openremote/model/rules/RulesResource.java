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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Rule", description = "Operations on rules")
@Path("rules")
public interface RulesResource {

    /**
     * Retrieve information about the global rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/global")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalEngineInfo", summary = "Retrieve information about the global rules engine")
    RulesEngineInfo getGlobalEngineInfo(@BeanParam RequestParams requestParams);

    /**
     * Retrieve information about the specified realm rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/realm/{realm}")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getRealmEngineInfo", summary = "Retrieve information about a realm rules engine")
    RulesEngineInfo getRealmEngineInfo(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * Retrieve information about the specified asset rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/asset/{assetId}")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getAssetEngineInfo", summary = "Retrieve information about an asset rules engine")
    RulesEngineInfo getAssetEngineInfo(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /**
     * Retrieve global rules. Only the superuser can perform this operation, a 403 status is returned if a regular user
     * tries to access global rulesets.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalRulesets", summary = " Retrieve the global rules")
    GlobalRuleset[] getGlobalRulesets(@BeanParam RequestParams requestParams, @QueryParam("language") List<Ruleset.Lang> languages, @QueryParam("fullyPopulate") boolean fullyPopulate);

    /**
     * Retrieve rules of a realm. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access rulesets outside of its authenticated realm. An empty result will be returned if the realm
     * can not be found.
     */
    @GET
    @Path("realm/for/{realm}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getRealmRulesets", summary = "Retrieve the rules of a realm")
    RealmRuleset[] getRealmRulesets(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @QueryParam("language") List<Ruleset.Lang> languages, @QueryParam("fullyPopulate") boolean fullyPopulate);

    /**
     * Retrieve rules of an asset. The superuser can retrieve rules of all realms and assets, a 403 status is returned
     * if a regular user tries to access ruleset outside of its authenticated realm, or if the user is restricted and
     * the asset is not one of its linked assets. An empty result will be returned if the asset can not be found.
     */
    @GET
    @Path("asset/for/{assetId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetRulesets", summary = "Retrieve the rules of an asset")
    AssetRuleset[] getAssetRulesets(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @QueryParam("language") List<Ruleset.Lang> languages, @QueryParam("fullyPopulate") boolean fullyPopulate);

    /* ################################################################################################# */

    /**
     * Create a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "createGlobalRuleset", summary = "Create a global ruleset")
    long createGlobalRuleset(@BeanParam RequestParams requestParams, @Valid GlobalRuleset ruleset);

    /**
     * Retrieve a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalRuleset", summary = "Retrieve a global ruleset")
    GlobalRuleset getGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a
     * regular user tries to access global ruleset.
     */
    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateGlobalRuleset", summary = "Update a global ruleset")
    void updateGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid GlobalRuleset ruleset);

    /**
     * Deletes a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @DELETE
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteGlobalRuleset", summary = "Delete a global ruleset")
    void deleteGlobalRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create a realm ruleset. The superuser can create rules in all realms, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @POST
    @Path("realm")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "createRealmRuleset", summary = "Create a realm ruleset")
    long createRealmRuleset(@BeanParam RequestParams requestParams, @Valid RealmRuleset ruleset);

    /**
     * Retrieve a realm ruleset. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @GET
    @Path("realm/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getRealmRuleset", summary = "Retrieve a realm ruleset")
    RealmRuleset getRealmRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update a realm ruleset. The superuser can update rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @PUT
    @Path("realm/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateRealmRuleset", summary = "Update a realm ruleset")
    void updateRealmRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid RealmRuleset ruleset);

    /**
     * Delete a realm ruleset. The superuser can delete rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @DELETE
    @Path("realm/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteRealmRuleset", summary = "Delete a realm ruleset")
    void deleteRealmRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

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
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "createAssetRuleset", summary = "Create an asset ruleset")
    long createAssetRuleset(@BeanParam RequestParams requestParams, @Valid AssetRuleset ruleset);

    /**
     * Retrieve an asset ruleset. The superuser can retrieve rules of all assets, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have
     * access to the asset.
     */
    @GET
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "getAssetRuleset", summary = "Retrieve an asset ruleset")
    AssetRuleset getAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Update an asset ruleset. The superuser can update rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @PUT
    @Path("asset/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateAssetRuleset", summary = "Update an asset ruleset")
    void updateAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id, @Valid AssetRuleset ruleset);

    /**
     * Delete an asset ruleset. The superuser can delete rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @DELETE
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteAssetRuleset", summary = "Delete an asset ruleset")
    void deleteAssetRuleset(@BeanParam RequestParams requestParams, @PathParam("id") Long id);

    /**
     * Get the geofences for the specified asset; if this method is accessed anonymously (public read) then the asset
     * must have public read enabled. Otherwise the asset must be linked to the logged in user.
     * If neither of these conditions are met then a 403 is returned.
     */
    @GET
    @Path("geofences/{assetId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetGeofences", summary = "Get the geofences of an asset")
    GeofenceDefinition[] getAssetGeofences(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);
}
