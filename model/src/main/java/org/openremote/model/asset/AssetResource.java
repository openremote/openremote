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
package org.openremote.model.asset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.AttributeWriteResult;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.value.MetaItemType;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Asset<?> access rules:
 * <ul>
 * <li>
 * The superuser (the admin in the master realm) may access all assets.
 * </li>
 * <li>
 * A regular user may have roles that allow read, write, or no access to any assets within
 * its authenticated realm.
 * </li>
 * <li>
 * A <em>restricted</em> user is linked to a subset of assets within its authenticated realm and
 * may have roles that allow read and/or write access to some asset details (see
 * {@link UserAssetLink}).
 * </li> </ul>
 * <p>
 * The only operations, always limited to linked assets, a restricted user is able to perform are:
 * <ul>
 * <li>{@link #getCurrentUserAssets}</li>
 * <li>{@link #queryAssets}</li>
 * <li>{@link #get}</li>
 * <li>{@link #update}</li>
 * <li>{@link #writeAttributeValue}</li>
 * </ul>
 */
@Tag(name = "Asset")
@Path("asset")
public interface AssetResource {

    @TsIgnore
    class Util {
        public static final String WRITE_ATTRIBUTE_HTTP_METHOD = "PUT";

        public static String getWriteAttributeUrl(AttributeRef attributeRef) {
            return "/asset/" + attributeRef.getId() + "/attribute/" + attributeRef.getName();
        }
    }

    // TODO This returns the same as #queryAssets, can it be removed?

    /**
     * Retrieve the linked assets of the currently authenticated user. If the request is made by the superuser, an empty
     * result is returned. If the request is made by a regular user, but the user has no linked assets and is therefore
     * not restricted, the assets without parent (root assets) of the authenticated realm are returned. Note that the
     * assets returned from this operation are not completely loaded and the {@link Asset#path} and {@link
     * Asset#attributes} are empty. Call {@link #get} to retrieve all asset details.
     */
    @GET
    @Path("user/current")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    Asset<?>[] getCurrentUserAssets(@BeanParam RequestParams requestParams);

    /**
     * Retrieve links between assets and users.
     * <p>
     * The <code>realm</code> parameter is required, <code>userId</code> and <code>assetId</code> can be null.
     * <p>
     * If the authenticated user is the superuser, assigned assets from any realm can be retrieved. Otherwise the
     * authenticated realm must be the same as the given realm. A 403 status is returned if a regular user tries to
     * get asset/user links in a realm different than its authenticated realm, or if the user is restricted. A 404
     * status is returned if the realm doesn't exist. A 400 status code is returned if the given user is not in
     * the given realm. An empty result is returned if the user or asset doesn't exist.
     * <p>
     * TODO: We could return the assets of a restricted user
     */
    @GET
    @Path("user/link")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    UserAssetLink[] getUserAssetLinks(@BeanParam RequestParams requestParams,
                                      @QueryParam("realm") String realm,
                                      @QueryParam("userId") String userId,
                                      @QueryParam("assetId") String assetId);

    /**
     * Create all of the specified links; they must all be for the same realm and user.
     * <p>
     * If the authenticated user is the superuser, asset/user links in any realm can be created. Otherwise assets
     * must be in the same realm as the authenticated user. A 403 status is returned if a regular user tries to create
     * an asset/user link in a realm different than its authenticated realm, or if the user is restricted. A
     * 400 status is returned if the user or asset or realm doesn't exist, or if the user is not in the realm.
     */
    @POST
    @Path("user/link")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void createUserAssetLinks(@BeanParam RequestParams requestParams, List<UserAssetLink> userAssets);

    /**
     * Delete a link between asset and user.
     * <p>
     * The <code>realm</code> is required.
     * <p>
     * If the authenticated user is the superuser, asset/user links from any realm can be deleted. Otherwise assets
     * must be in the same realm as the authenticated user. A 403 status is returned if a regular user tries to delete
     * an asset/user link in a realm different than its authenticated realm, or if the user is restricted. A
     * 400 status is returned if the user or asset or realm doesn't exist.
     */
    @DELETE
    @Path("user/link/{realm}/{userId}/{assetId}")
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void deleteUserAssetLink(@BeanParam RequestParams requestParams,
                             @PathParam("realm") String realm,
                             @PathParam("userId") String userId,
                             @PathParam("assetId") String assetId);

    /**
     * Delete all of the specified links; they must all be for the same realm and user.
     * <p>
     * If the authenticated user is the superuser, asset/user links from any realm can be deleted. Otherwise assets
     * must be in the same realm as the authenticated user. A 403 status is returned if a regular user tries to delete
     * an asset/user link in a realm different than its authenticated realm, or if the user is restricted. A
     * 400 status is returned if the user or asset or realm doesn't exist.
     */
    @POST
    @Path("user/link/delete")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void deleteUserAssetLinks(@BeanParam RequestParams requestParams, List<UserAssetLink> userAssets);

    @DELETE
    @Path("user/link/{realm}/{userId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void deleteAllUserAssetLinks(@BeanParam RequestParams requestParams,
                                 @PathParam("realm") String realm,
                                 @PathParam("userId") String userId);

    /**
     * Retrieve the asset. Regular users can only access assets in their authenticated realm, the superuser can access
     * assets in other (all) realms. A 403 status is returned if a regular user tries to access an asset in a realm
     * different than its authenticated realm, or if the user is restricted and the asset is not linked to the user. All
     * asset details (path, attributes) will be populated, the asset is loaded completely.
     */
    @GET
    @Path("{assetId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    Asset<?> get(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /**
     * Same as {@link #get} but only returns a partially loaded asset (no attributes or path)
     */
    @GET
    @Path("partial/{assetId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    Asset<?> getPartial(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /**
     * Updates the asset. Regular users can only update assets in their authenticated realm, the superuser can update
     * assets in other (all) realms. A 403 status is returned if a regular user tries to update an asset in a realm
     * different than its authenticated realm, or if the original or target realm is not accessible. A 403 status is
     * returned if the user is restricted and the asset is not linked to the user. A 400 status is returned if the
     * asset's parent doesn't exist. A 400 status is returned if a restricted user attempts to write private meta items
     * of any attributes. If a restricted user tries to write asset properties or dynamic attributes or
     * meta items of dynamic attributes which are not writable by a restricted user, such data is ignored. For more
     * details on limitations of restricted users, see {@link UserAssetLink}.
     */
    @PUT
    @Path("{assetId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void update(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @Valid Asset<?> asset);

    /**
     * Updates an attribute of an asset. Regular users can only update assets in their authenticated realm, the
     * superuser can update assets in other (all) realms. A 403 status is returned if a regular user tries to update an
     * asset in a realm different than its authenticated realm, or if the user is restricted and the asset to update is
     * not in the set of linked assets of the restricted user.
     * <p>
     * If the asset or attribute doesn't exist then a 404 status is returned.
     * <p>
     * If an attribute is marked as {@link MetaItemType#ACCESS_PUBLIC_WRITE} then the attribute can be written publicly
     * <p>
     * This operation is ultimately asynchronous, any call will return before the actual attribute value is changed in
     * any storage or downstream processors. Thus any constraint violation or processing error will not be returned from
     * this method, query the system later to determine the actual state and outcome of the write operation. The version
     * of the asset entity will not be incremented by this operation, thus concurrent updates can overwrite data
     * undetected ("last commit wins").
     */
    @PUT
    @Path("{assetId}/attribute/{attributeName}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Write to a single attribute", responses = {
        @ApiResponse(
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AttributeWriteResult.class)))})
    Response writeAttributeValue(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @PathParam("attributeName") String attributeName, Object value);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("attributes")
    AttributeWriteResult[] writeAttributeValues(@BeanParam RequestParams requestParams, AttributeState[] attributeStates);

    /**
     * Creates an asset. The identifier value of the asset can be provided, it should be a globally unique string value,
     * and must be 22 characters long. If no identifier value is provided, a unique value will be generated by
     * the system upon insert. Regular users can only create assets in their authenticated realm, the superuser can
     * create assets in other (all) realms. A 403 status is returned if a regular user tries to create an asset in a
     * realm different than its authenticated realm, or if the user is restricted. A 400 status is returned if the
     * asset's parent or realm doesn't exist or if an ID is provided and an asset with this ID already exists.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    Asset<?> create(@BeanParam RequestParams requestParams, @Valid Asset<?> asset);

    /**
     * Deletes an asset. Regular users can only delete assets in their authenticated realm, the superuser can delete
     * assets in other (all) realms. A 403 status is returned if a regular user tries to delete an asset in a realm
     * different than its authenticated realm, or if the user is restricted.
     */
    @DELETE
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void delete(@BeanParam RequestParams requestParams, @QueryParam("assetId") List<String> assetIds);

    /**
     * Retrieve assets using an {@link AssetQuery}.
     * <p>
     * If the authenticated user is the superuser then assets referenced in the query or returned by the query can be in
     * any realm. Otherwise assets must be in the same realm as the authenticated user, and for a restricted user, the
     * assets must be linked to the user. An empty result is returned if the user does not have access to the assets.
     * What is populated on the returned assets is determined by the
     * {@link AssetQuery#select} value.
     */
    @POST
    @Path("query")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Asset<?>[] queryAssets(@BeanParam RequestParams requestParams, AssetQuery query);

    /**
     * Change parent for a set of asset
     */
    @PUT
    @Path("{parentAssetId}/child")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    void updateParent(@BeanParam RequestParams requestParams, @PathParam("parentAssetId") @NotNull(message = "Parent reference required") String parentId, @QueryParam("assetIds") @Size(min = 1, message = "At least one child to update parent reference") List<String> assetIds);

    /**
     * Remove parent reference from each asset referenced in the query parameter assetIds
     */
    @DELETE
    @Path("/parent")
    @Produces(APPLICATION_JSON)
    void updateNoneParent(@BeanParam RequestParams requestParams, @QueryParam("assetIds") @Size(min = 1, message = "At least one child to update parent reference") List<String> assetIds);
}
