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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;
import static org.openremote.model.http.OpenApiExamples.ASSET_RULESET_CREATE;

@Tag(name = "Rule", description = "Inspect rule engines and query or manage global, realm, and asset rulesets")
@Path("rules")
public interface RulesResource {

    /**
     * Retrieve information about the global rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/global")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalEngineInfo", summary = "Retrieve information about the global rules engine",
        description = "Returns runtime status and compilation/execution error counts for the global engine, or null when that engine is not running. Super-user access is required.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @ApiResponse(responseCode = "204", description = "The global rules engine is not running")
    RulesEngineInfo getGlobalEngineInfo(@BeanParam RequestParams requestParams);

    /**
     * Retrieve information about the specified realm rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/realm/{realm}")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getRealmEngineInfo", summary = "Retrieve information about a realm rules engine",
        description = "Returns runtime status and error counts for an accessible realm engine, or null when that engine is not running. Restricted users cannot inspect realm engines.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @ApiResponse(responseCode = "204", description = "The requested realm rules engine is not running")
    RulesEngineInfo getRealmEngineInfo(@BeanParam RequestParams requestParams,
                                       @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm);

    /**
     * Retrieve information about the specified asset rules engine (if engine doesn't exist then will return null).
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info/asset/{assetId}")
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getAssetEngineInfo", summary = "Retrieve information about an asset rules engine",
        description = "Returns runtime status and error counts for an accessible asset engine. Returns null when the asset or its engine does not exist.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @ApiResponse(responseCode = "204", description = "The asset or its rules engine does not exist")
    RulesEngineInfo getAssetEngineInfo(@BeanParam RequestParams requestParams,
                                       @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId);

    /**
     * Retrieve global rules. Only the superuser can perform this operation, a 403 status is returned if a regular user
     * tries to access global rulesets.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalRulesets", summary = "Retrieve global rulesets",
        description = "Returns global rulesets filtered by repeated language parameters. fullyPopulate controls whether rule source and other large fields are loaded; super-user access is required.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    GlobalRuleset[] getGlobalRulesets(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Rule languages to include; repeat the query parameter for multiple values.", style = ParameterStyle.FORM, explode = Explode.TRUE) @QueryParam("language") List<Ruleset.Lang> languages,
        @Parameter(description = "Include rule source, meta, and other large fields instead of summary records.", example = "true") @QueryParam("fullyPopulate") boolean fullyPopulate);

    /**
     * Retrieve rules of a realm. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access rulesets outside of its authenticated realm. An empty result will be returned if the realm
     * can not be found.
     */
    @GET
    @Path("realm/for/{realm}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getRealmRulesets", summary = "Retrieve rulesets for a realm",
        description = "Returns realm rulesets filtered by language. Anonymous, restricted, or callers without read-rules permission receive public rulesets only; fullyPopulate controls loading of rule source.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Forbidden
    RealmRuleset[] getRealmRulesets(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = "Rule languages to include; repeat the query parameter for multiple values.", style = ParameterStyle.FORM, explode = Explode.TRUE) @QueryParam("language") List<Ruleset.Lang> languages,
        @Parameter(description = "Include rule source, meta, and other large fields instead of summary records.", example = "true") @QueryParam("fullyPopulate") boolean fullyPopulate);

    /**
     * Retrieve rules of an asset. The superuser can retrieve rules of all realms and assets, a 403 status is returned
     * if a regular user tries to access ruleset outside of its authenticated realm, or if the user is restricted and
     * the asset is not one of its linked assets. An empty result will be returned if the asset can not be found.
     */
    @GET
    @Path("asset/for/{assetId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetRulesets", summary = "Retrieve rulesets for an asset",
        description = "Returns asset rulesets filtered by language. Callers without full access receive public rulesets only; a nonexistent asset produces an empty array.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Forbidden
    AssetRuleset[] getAssetRulesets(
        @BeanParam RequestParams requestParams,
        @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId,
        @Parameter(description = "Rule languages to include; repeat the query parameter for multiple values.", style = ParameterStyle.FORM, explode = Explode.TRUE) @QueryParam("language") List<Ruleset.Lang> languages,
        @Parameter(description = "Include rule source, meta, and other large fields instead of summary records.", example = "true") @QueryParam("fullyPopulate") boolean fullyPopulate);

    /* ################################################################################################# */

    /**
     * Create a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "createGlobalRuleset", summary = "Create a global ruleset",
        description = "Creates and deploys a global ruleset and returns its numeric ID. Super-user access is required; legacy JavaScript rulesets cannot be created.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    long createGlobalRuleset(@BeanParam RequestParams requestParams,
                             @Valid @RequestBody(required = true, description = "Global ruleset name, language, source, enabled state, and optional metadata.") GlobalRuleset ruleset);

    /**
     * Retrieve a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getGlobalRuleset", summary = "Retrieve a global ruleset",
        description = "Returns one fully populated global ruleset including transient deployment status and errors. Super-user access is required.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    GlobalRuleset getGlobalRuleset(@BeanParam RequestParams requestParams,
                                   @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

    /**
     * Update a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a
     * regular user tries to access global ruleset.
     */
    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateGlobalRuleset", summary = "Update a global ruleset",
        description = "Replaces and redeploys the identified global ruleset. Path and body IDs must match; legacy JavaScript rulesets cannot be updated.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateGlobalRuleset(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Numeric ruleset identifier; must match the body id.", example = "27") @PathParam("id") Long id,
        @Valid @RequestBody(required = true, description = "Complete replacement global ruleset; its id must match the path.") GlobalRuleset ruleset);

    /**
     * Deletes a global ruleset. Only the superuser can perform this operation, a 403 status is returned if a regular
     * user tries to access global ruleset.
     */
    @DELETE
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteGlobalRuleset", summary = "Delete a global ruleset",
        description = "Permanently removes and undeploys one global ruleset. Super-user access is required.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    void deleteGlobalRuleset(@BeanParam RequestParams requestParams,
                             @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

    /* ################################################################################################# */

    /**
     * Create a realm ruleset. The superuser can create rules in all realms, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @POST
    @Path("realm")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "createRealmRuleset", summary = "Create a realm ruleset",
        description = "Creates and deploys a ruleset in its body realm and returns its numeric ID. Restricted users cannot create realm rulesets; legacy JavaScript is not accepted.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    long createRealmRuleset(@BeanParam RequestParams requestParams,
                            @Valid @RequestBody(required = true, description = "Realm ruleset including its target realm, name, language, source, and enabled state.") RealmRuleset ruleset);

    /**
     * Retrieve a realm ruleset. The superuser can retrieve rules of all realms, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted.
     */
    @GET
    @Path("realm/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @Operation(operationId = "getRealmRuleset", summary = "Retrieve a realm ruleset",
        description = "Returns one fully populated realm ruleset when its realm is accessible to the caller.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    RealmRuleset getRealmRuleset(@BeanParam RequestParams requestParams,
                                 @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

    /**
     * Update a realm ruleset. The superuser can update rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @PUT
    @Path("realm/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateRealmRuleset", summary = "Update a realm ruleset",
        description = "Replaces and redeploys one realm ruleset. Path and body IDs and the existing realm must match; legacy JavaScript cannot be updated.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateRealmRuleset(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Numeric ruleset identifier; must match the body id.", example = "27") @PathParam("id") Long id,
        @Valid @RequestBody(required = true, description = "Complete replacement realm ruleset; its id and realm must match the existing ruleset.") RealmRuleset ruleset);

    /**
     * Delete a realm ruleset. The superuser can delete rules of all realms, a 403 status is returned if a regular user
     * tries to access rulesets outside of its authenticated realm, or if the user is restricted.
     */
    @DELETE
    @Path("realm/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteRealmRuleset", summary = "Delete a realm ruleset",
        description = "Permanently removes and undeploys one ruleset from an accessible realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    void deleteRealmRuleset(@BeanParam RequestParams requestParams,
                            @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

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
    @Operation(operationId = "createAssetRuleset", summary = "Create an asset ruleset",
        description = "Creates and deploys a ruleset for an accessible asset and returns its numeric ID. The assetId is taken from the request body.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    long createAssetRuleset(@BeanParam RequestParams requestParams,
                            @Valid @RequestBody(required = true, description = "Asset ruleset including its target assetId, name, language, source, and enabled state.",
                                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AssetRuleset.class),
                                    examples = @ExampleObject(name = "Asset alert ruleset", summary = "Create an enabled JSON ruleset for one asset", value = ASSET_RULESET_CREATE))) AssetRuleset ruleset);

    /**
     * Retrieve an asset ruleset. The superuser can retrieve rules of all assets, a 403 status is returned if a regular
     * user tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have
     * access to the asset.
     */
    @GET
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "getAssetRuleset", summary = "Retrieve an asset ruleset",
        description = "Returns one fully populated asset ruleset when the caller can access its realm and owning asset.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    AssetRuleset getAssetRuleset(@BeanParam RequestParams requestParams,
                                 @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

    /**
     * Update an asset ruleset. The superuser can update rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @PUT
    @Path("asset/{id}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "updateAssetRuleset", summary = "Update an asset ruleset",
        description = "Replaces and redeploys one asset ruleset. The ID must match and an existing ruleset cannot be reassigned to another asset.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateAssetRuleset(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Numeric ruleset identifier; must match the body id.", example = "27") @PathParam("id") Long id,
        @Valid @RequestBody(required = true, description = "Complete replacement asset ruleset; its id and assetId must match the existing ruleset.") AssetRuleset ruleset);

    /**
     * Delete an asset ruleset. The superuser can delete rules of all assets, a 403 status is returned if a regular user
     * tries to access ruleset outside of its authenticated realm, or if the user is restricted and does not have access
     * to the asset.
     */
    @DELETE
    @Path("asset/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_RULES_ROLE})
    @Operation(operationId = "deleteAssetRuleset", summary = "Delete an asset ruleset",
        description = "Permanently removes and undeploys one ruleset from an accessible asset.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    void deleteAssetRuleset(@BeanParam RequestParams requestParams,
                            @Parameter(description = "Numeric ruleset identifier.", example = "27") @PathParam("id") Long id);

    /**
     * Get the geofences for the specified asset; if this method is accessed anonymously (public read) then the asset
     * must have public read enabled. Otherwise the asset must be linked to the logged in user.
     * If neither of these conditions are met then a 403 is returned.
     */
    @GET
    @Path("geofences/{assetId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetGeofences", summary = "Retrieve geofences for an asset",
        description = "Extracts geofence definitions relevant to the asset. Anonymous access requires a public-readable asset; authenticated restricted users require an asset link.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Forbidden
    @OpenApiResponses.NotFound
    GeofenceDefinition[] getAssetGeofences(@BeanParam RequestParams requestParams,
                                           @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId);
}
