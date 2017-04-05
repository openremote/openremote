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
package org.openremote.manager.shared.asset;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.model.asset.Asset;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Asset access rules:
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
 * may have roles that allow read and/or write access to protected asset details (see
 * {@link org.openremote.model.asset.UserAsset}).
 * <p>
 * The only operations a restricted user is able to perform are {@link #getCurrentUserAssets},
 * {@link #get}, {@link #update}, {@link #readAttributeValue} and {@link #writeAttributeValue}.
 * </li>
 * </ul>
 */
@Path("asset")
@JsType(isNative = true)
public interface AssetResource {

    /**
     * Retrieve the linked assets of the currently authenticated user. If the request is made
     * by the superuser, an empty result is returned. If the request is made by a regular user,
     * but the user has no linked assets and is therefore not restricted, the assets without
     * parent (root assets) of the authenticated realm are returned. Note that the assets returned
     * from this operation are not completely loaded and the {@link Asset#path} and
     * {@link Asset#attributes} are empty. Call {@link #get} to retrieve all asset details.
     */
    @GET
    @Path("user/current")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Asset[] getCurrentUserAssets(@BeanParam RequestParams requestParams);

    /**
     * Retrieve the assets without parent (root assets) of the given realm, or if the realm argument
     * is empty, of the authenticated realm. Regular users can only access assets in their authenticated
     * realm. The superuser can access assets in other (all) realms. An empty result is returned if the user
     * does not have access to the assets or if the user is restricted. Note that the assets returned
     * from this operation are not completely loaded and the {@link Asset#path} and
     * {@link Asset#attributes} are empty. Call {@link #get} to retrieve all asset details.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Asset[] getRoot(@BeanParam RequestParams requestParams, @QueryParam("realmId") String realmId);

    /**
     * Retrieve the child assets of the given parent asset. If the authenticated user is the superuser,
     * parent and child assets can be in any realm. Otherwise, assets must in the same realm as the
     * authenticated user. An empty result is returned if the user does not have access to the assets
     * or if the user is restricted. Note that the assets returned from this operation are not completely
     * loaded and the {@link Asset#path} and {@link Asset#attributes} are empty. Call {@link #get} to
     * retrieve all asset details.
     */
    @GET
    @Path("{assetId}/children")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Asset[] getChildren(@BeanParam RequestParams requestParams, @PathParam("assetId") String parentId);

    /**
     * Retrieve the asset. Regular users can only access assets in their authenticated realm,
     * the superuser can access assets in other (all) realms. A 403 status is returned if a regular
     * user tries to access an asset in a realm different than its authenticated realm, or if the
     * user is restricted and the asset is not linked to the user.
     */
    @GET
    @Path("{assetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Asset get(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    /**
     * Updates the asset. Regular users can only update assets in their authenticated realm,
     * the superuser can update assets in other (all) realms. A 403 status is returned if a regular
     * user tries to update an asset in a realm different than its authenticated realm, or if the
     * user is restricted and the asset is not linked to the user. A 400 status is returned if
     * the asset's parent or realm doesn't exist.
     */
    @PUT
    @Path("{assetId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void update(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @Valid Asset asset);

    /**
     * Updates an attribute of an asset. Regular users can only update assets in
     * their authenticated realm, the superuser can update assets in other (all) realms. A 403 status
     * is returned if a regular user tries to update an asset in a realm different than its
     * authenticated realm, or if the user is restricted and the asset to update is not in the set of linked
     * assets of the restricted user. A 400 status is returned if the update was not successful, e.g. because
     * the given value does not match the attribute's type. Note that this update does not increment the version
     * of an asset entity, thus concurrent updates can overwrite data undetected ("last commit wins").
     */
    @PUT
    @Path("{assetId}/attribute/{attributeName}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void writeAttributeValue(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @PathParam("attributeName") String attributeName, String rawJson);

    /**
     * Reads the attribute state of an asset. Regular users can only access assets in
     * their authenticated realm, the superuser can access assets in other (all) realms. A 403 status
     * is returned if a regular user tries to access an asset in a realm different than its
     * authenticated realm, or if the user is restricted and the asset to read is not in the set of linked
     * assets of the restricted user.
     */
    @GET
    @Path("{assetId}/attribute/{attributeName}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    String readAttributeValue(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId, @PathParam("attributeName") String attributeName);

    /**
     * Creates an asset. The identifier value of the asset can be provided, it should be a
     * globally unique string value, and must be at least 22 characters long. If no identifier
     * value is provided, a unique value will be generated by the system upon insert. Regular
     * users can only create assets in their authenticated realm, the superuser can create
     * assets in other (all) realms. A 403 status is returned if a regular user tries to create
     * an asset in a realm different than its authenticated realm, or if the user is restricted.
     * A 400 status is returned if the asset's parent or realm doesn't exist.
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void create(@BeanParam RequestParams requestParams, @Valid Asset asset);

    /**
     * Deletes an asset. Regular users can only delete assets in their authenticated realm,
     * the superuser can delete assets in other (all) realms. A 403 status is returned if a regular
     * user tries to delete an asset in a realm different than its authenticated realm, or if the
     * user is restricted. A 400 status code is returned if the asset has children and therefore
     * can't be deleted.
     */
    @DELETE
    @Path("{assetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void delete(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);
}
