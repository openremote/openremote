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
package org.openremote.manager.asset;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.*;
import org.openremote.model.asset.BaseAssetQuery.Select;
import org.openremote.model.attribute.*;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.Tenant;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;
import org.openremote.model.value.Values;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.*;
import static org.openremote.container.Container.JSON;
import static org.openremote.model.asset.AssetQuery.*;
import static org.openremote.model.attribute.AttributeEvent.Source.CLIENT;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetResourceImpl extends ManagerWebResource implements AssetResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());

    protected final static Asset[] EMPTY_ASSETS = new Asset[0];
    protected final AssetStorageService assetStorageService;
    protected final MessageBrokerService messageBrokerService;

    public AssetResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             AssetStorageService assetStorageService,
                             MessageBrokerService messageBrokerService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.messageBrokerService = messageBrokerService;
    }

    @Override
    public Asset[] getCurrentUserAssets(RequestParams requestParams) {
        try {
            if (isSuperUser()) {
                return new Asset[0];
            }

            if (!isRestrictedUser()) {
                List<ServerAsset> result = assetStorageService.findAll(
                    new AssetQuery()
                        .parent(new ParentPredicate(true))
                        .tenant(new TenantPredicate().realm(getAuthenticatedRealm()))
                );
                return result.toArray(new Asset[result.size()]);
            }

            List<ServerAsset> assets = assetStorageService.findAll(
                new AssetQuery().select(
                    new Select(Include.ALL_EXCEPT_PATH_AND_ATTRIBUTES, Access.RESTRICTED_READ)
                ).userId(getUserId())
            );

            // Filter assets that might have been moved into a different realm and can no longer be accessed by user
            // TODO: Should we forbid moving assets between realms?
            Tenant authenticatedTenant = getAuthenticatedTenant();
            Iterator<ServerAsset> it = assets.iterator();
            while (it.hasNext()) {
                ServerAsset asset = it.next();
                if (!asset.getRealmId().equals(authenticatedTenant.getId())) {
                    LOG.warning("User '" + getUsername() + "' linked to asset in other realm, skipping: " + asset);
                    it.remove();
                }
            }

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return assets.toArray(new ServerAsset[assets.size()]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public UserAsset[] getUserAssetLinks(RequestParams requestParams, String realmId, String userId, String assetId) {
        try {
            if (realmId == null)
                throw new WebApplicationException(BAD_REQUEST);

            Tenant tenant = identityService.getIdentityProvider().getTenantForRealmId(realmId);
            if (tenant == null)
                throw new WebApplicationException(NOT_FOUND);

            if (!isSuperUser() && (isRestrictedUser() && !getAuthenticatedTenant().getId().equals(tenant.getId())))
                throw new WebApplicationException(FORBIDDEN);

            if (userId != null && !identityService.getIdentityProvider().isUserInTenant(userId, realmId))
                throw new WebApplicationException(BAD_REQUEST);

            UserAsset[] result = assetStorageService.findUserAssets(realmId, userId, assetId).toArray(new UserAsset[0]);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result;

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }


    @Override
    public void createUserAsset(RequestParams requestParams, UserAsset userAsset) {
        String realmId = userAsset.getId().getRealmId();
        String userId = userAsset.getId().getUserId();
        String assetId = userAsset.getId().getAssetId();

        if (!identityService.getIdentityProvider().isUserInTenant(userId, realmId))
            throw new WebApplicationException(BAD_REQUEST);

        ServerAsset asset;
        if ((asset = assetStorageService.find(assetId)) == null || !asset.getRealmId().equals(realmId)) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        if (isSuperUser()) {
            assetStorageService.storeUserAsset(userAsset);
            return;
        }

        // Restricted users or regular users in a different realm can not create links
        if (isRestrictedUser()
            || !getAuthenticatedTenant().getId().equals(realmId))
            throw new WebApplicationException(FORBIDDEN);

        assetStorageService.storeUserAsset(userAsset);
    }

    @Override
    public void deleteUserAsset(RequestParams requestParams, String realmId, String userId, String assetId) {
        if (!identityService.getIdentityProvider().isUserInTenant(userId, realmId))
            throw new WebApplicationException(BAD_REQUEST);

        if (isSuperUser()) {
            assetStorageService.deleteUserAsset(realmId, userId, assetId);
            return;
        }

        // Restricted users or regular users in a different realm can not delete links
        if (isRestrictedUser()
            || !getAuthenticatedTenant().getId().equals(realmId))
            throw new WebApplicationException(FORBIDDEN);

        assetStorageService.deleteUserAsset(realmId, userId, assetId);
    }

    @Override
    public Asset get(RequestParams requestParams, String assetId) {
        try {
            ServerAsset asset;

            // Check restricted
            if (isRestrictedUser()) {
                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(FORBIDDEN);
                }
                asset = assetStorageService.find(assetId, true, Access.RESTRICTED_READ);
            } else {
                asset = assetStorageService.find(assetId, true);
            }

            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "': " + asset);
                throw new WebApplicationException(FORBIDDEN);
            }

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return asset;

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public void update(RequestParams requestParams, String assetId, Asset asset) {
        try {
            ServerAsset serverAsset = assetStorageService.find(assetId, true);
            if (serverAsset == null)
                throw new WebApplicationException(NOT_FOUND);

            // Current and new realm of asset must be accessible
            if (!isTenantActiveAndAccessible(serverAsset) || !isTenantActiveAndAccessible(asset)) {
                LOG.fine("Current or new tenant not accessible by user '" + getUsername() + "', can't update: " + serverAsset);
                throw new WebApplicationException(FORBIDDEN);
            }

            boolean isRestrictedUser = isRestrictedUser();

            // The asset that will ultimately be stored (override/ignore some values for restricted users)
            ServerAsset resultAsset = ServerAsset.map(
                asset,
                serverAsset,
                isRestrictedUser ? serverAsset.getName() : null, // TODO We could allow restricted users to update names?
                isRestrictedUser ? serverAsset.getRealmId() : null, // Restricted users can not change realm
                isRestrictedUser ? serverAsset.getParentId() : null, // Restricted users can not change realm
                serverAsset.getType(), // The type an never change
                null, // Anybody can set a new location
                isRestrictedUser ? serverAsset.isAccessPublicRead() : null, // Restricted user can not change access public flag
                isRestrictedUser ? serverAsset.getAttributes() : null // Restricted users need manual attribute merging (see below)
            );

            // For restricted users, merge existing and updated attributes depending on write permissions
            if (isRestrictedUser) {

                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(FORBIDDEN);
                }

                // Merge updated with existing attributes
                for (AssetAttribute updatedAttribute : asset.getAttributesList()) {

                    // Proper validation happens on merge(), here we only need the name to continue
                    String updatedAttributeName = updatedAttribute.getNameOrThrow();

                    // Check if attribute is present on the asset in storage
                    Optional<AssetAttribute> serverAttribute = resultAsset.getAttribute(updatedAttributeName);
                    if (serverAttribute.isPresent()) {
                        AssetAttribute existingAttribute = serverAttribute.get();

                        // If the existing attribute is not writable by restricted client, ignore it
                        if (!existingAttribute.isAccessRestrictedWrite()) {
                            LOG.fine("Existing attribute not writable by restricted client, ignoring update of: " + updatedAttributeName);
                            continue;
                        }

                        // Merge updated with existing meta items (modifying a copy)
                        Meta updatedMetaItems = updatedAttribute.getMeta();
                        Meta existingMetaItems = existingAttribute.getMeta().copy();

                        // Remove any writable existing meta items
                        existingMetaItems.removeIf(AssetModel::isMetaItemRestrictedWrite);

                        // Add any writable updated meta items
                        updatedMetaItems.stream().filter(AssetModel::isMetaItemRestrictedWrite).forEach(existingMetaItems::add);

                        // Replace existing with updated attribute
                        updatedAttribute.setMeta(existingMetaItems);
                        resultAsset.replaceAttribute(updatedAttribute);

                    } else {

                        // An attribute added by a restricted user can only have meta items which are writable
                        updatedAttribute.getMetaStream().forEach(metaItem -> {
                            if (!AssetModel.isMetaItemRestrictedWrite(metaItem)) {
                                LOG.fine("Attribute has " + metaItem + " not writable by restricted client: " + updatedAttributeName);
                                throw new WebApplicationException(
                                    "Attribute has meta item not writable by restricted client: " + updatedAttributeName,
                                    BAD_REQUEST
                                );
                            }
                        });

                        // An attribute added by a restricted user must be readable by restricted users
                        if (!updatedAttribute.isAccessRestrictedRead()) {
                            updatedAttribute.addMeta(new MetaItem(AssetMeta.ACCESS_RESTRICTED_READ, Values.create(true)));
                        }

                        // An attribute added by a restricted user must be writable by restricted users
                        if (!updatedAttribute.isAccessRestrictedWrite()) {
                            updatedAttribute.addMeta(new MetaItem(AssetMeta.ACCESS_RESTRICTED_WRITE, Values.create(true)));
                        }

                        // Add the new attribute
                        resultAsset.addAttributes(updatedAttribute);
                    }
                }

                // Remove missing attributes
                resultAsset.getAttributesList().removeIf(existingAttribute ->
                    existingAttribute.getName().isPresent()
                        && !asset.hasAttribute(existingAttribute.getName().get()) && existingAttribute.isAccessRestrictedWrite()
                );
            }

            // If attribute is type RULES_TEMPLATE_FILTER, enforce meta item RULE_STATE
            // TODO Only done for update(Asset) and not create(Asset) as we don't need that right now
            // TODO Implement "Saved Filter/Searches" properly, allowing restricted users to create rule state flags is not great
            resultAsset.getAttributesStream().forEach(attribute -> {
                if (attribute.getType().map(attributeType -> attributeType == AttributeType.RULES_TEMPLATE_FILTER).orElse(false)
                    && !attribute.hasMetaItem(AssetMeta.RULE_STATE)) {
                    attribute.addMeta(new MetaItem(AssetMeta.RULE_STATE, Values.create(true)));
                }
            });

            // Store the result
            assetStorageService.merge(resultAsset, isRestrictedUser ? getUsername() : null);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public void writeAttributeValue(RequestParams requestParams, String assetId, String attributeName, String rawJson) {
        try {
            try {
                Value value = Values.instance()
                    .parse(rawJson)
                    .orElse(null); // When parsing literal JSON "null"

                AttributeEvent event = new AttributeEvent(
                    new AttributeRef(assetId, attributeName), value, timerService.getCurrentTimeMillis()
                );

                // Process asynchronously but block for a little while waiting for the result
                Map<String, Object> headers = new HashMap<>();
                headers.put(AttributeEvent.HEADER_SOURCE, CLIENT);
                headers.put(Constants.AUTH_CONTEXT, getAuthContext());
                Object result = messageBrokerService.getProducerTemplate().requestBodyAndHeaders(
                    AssetProcessingService.ASSET_QUEUE, event, headers
                );

                if (result instanceof AssetProcessingException) {
                    AssetProcessingException processingException = (AssetProcessingException) result;
                    switch (processingException.getReason()) {
                        case ILLEGAL_SOURCE:
                        case NO_AUTH_CONTEXT:
                        case INSUFFICIENT_ACCESS:
                            throw new WebApplicationException(FORBIDDEN);
                        case ASSET_NOT_FOUND:
                        case ATTRIBUTE_NOT_FOUND:
                            throw new WebApplicationException(NOT_FOUND);
                        case INVALID_AGENT_LINK:
                        case ILLEGAL_AGENT_UPDATE:
                        case INVALID_ATTRIBUTE_EXECUTE_STATUS:
                            throw new IllegalStateException(processingException);
                        default:
                            throw processingException;
                    }
                }

            } catch (ValueException ex) {
                throw new IllegalStateException("Error parsing JSON", ex);
            }

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset create(RequestParams requestParams, Asset asset) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(FORBIDDEN);
            }

            if (asset == null) {
                LOG.fine("No asset in request");
                throw new WebApplicationException(BAD_REQUEST);
            }

            // If there was no realm provided (create was called by regular user in manager UI), use the auth realm
            if (asset.getRealmId() == null || asset.getRealmId().length() == 0) {
                asset.setRealmId(getAuthenticatedTenant().getId());
            }

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't create: " + asset);
                throw new WebApplicationException(FORBIDDEN);
            }

            ServerAsset serverAsset = ServerAsset.map(asset, new ServerAsset());

            // Allow client to set identifier
            // TODO Instead should return asset identifier instead of NO CONTENT
            if (asset.getId() != null) {
                // At least some sanity check, we must hope that the client has set a unique ID
                if (asset.getId().length() < 22) {
                    LOG.fine("Identifier value is too short, can't persist asset: " + asset);
                    throw new WebApplicationException(BAD_REQUEST);
                }
                serverAsset.setId(asset.getId());
            }

            return assetStorageService.merge(serverAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String assetId) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(FORBIDDEN);
            }
            ServerAsset asset = assetStorageService.find(assetId, true);
            if (asset == null)
                return;

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't delete: " + asset);
                throw new WebApplicationException(FORBIDDEN);
            }
            if (!assetStorageService.delete(assetId)) {
                throw new WebApplicationException(BAD_REQUEST);
            }
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset[] queryAssets(RequestParams requestParams, AssetQuery query) {
        try {
            if (query == null) {
                return EMPTY_ASSETS;
            }

            if (isRestrictedUser()) {
                // A restricted user can only query linked assets
                query = query.userId(getUserId());

                // A restricted user may not query private asset data, only restricted or public
                if (query.select == null)
                    query.select = new Select();
                if (query.select.access == null || query.select.access == Access.PRIVATE_READ)
                    query.select.filterAccess(Access.RESTRICTED_READ);
            }

            Tenant tenant = query.tenant != null
                ? !isNullOrEmpty(query.tenant.realmId)
                ? identityService.getIdentityProvider().getTenantForRealmId(query.tenant.realmId)
                : !isNullOrEmpty(query.tenant.realm)
                ? identityService.getIdentityProvider().getTenantForRealm(query.tenant.realm)
                : getAuthenticatedTenant()
                : getAuthenticatedTenant();

            if (tenant == null) {
                throw new WebApplicationException(NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(tenant)) {
                return EMPTY_ASSETS;
            }

            // This replicates behaviour of old getRoot and getChildren methods
            if (!isSuperUser() || query.parent == null || query.parent.noParent) {
                query.tenant(new TenantPredicate(tenant.getId()));
            }

            List<ServerAsset> result = assetStorageService.findAll(query);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result.toArray(new Asset[result.size()]);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset[] queryPublicAssets(RequestParams requestParams, AssetQuery query) {

        String requestRealm = getRequestRealm();

        if (query == null || TextUtil.isNullOrEmpty(requestRealm)) {
            return EMPTY_ASSETS;
        }

        // Force realm to be request realm
        if (query.tenant == null) {
            query.tenant(new TenantPredicate().realm(requestRealm));
        } else {
            query.tenant.realm = requestRealm;
        }

        // Force public access filter on query
        if (query.select == null) {
            query.select(new Select().filterAccess(Access.PUBLIC_READ));
        } else {
            query.select.filterAccess(Access.PUBLIC_READ);
        }

        try {
            List<ServerAsset> result = assetStorageService.findAll(query);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result.toArray(new Asset[result.size()]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset[] getPublicAssets(RequestParams requestParams, String q) {
        AssetQuery assetQuery;
        try {
            assetQuery = JSON.readValue(q, AssetQuery.class);
        } catch (IOException ex) {
            throw new WebApplicationException("Error parsing query parameter 'q' as JSON object", BAD_REQUEST);
        }

        Asset[] result = queryPublicAssets(requestParams, assetQuery);

        // Compress response (the request attribute enables the interceptor)
        request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

        return result;
    }
}
