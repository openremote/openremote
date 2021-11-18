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

import com.fasterxml.jackson.databind.node.NullNode;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.attribute.*;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.persistence.OptimisticLockException;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static javax.ws.rs.core.Response.Status.*;
import static org.openremote.model.attribute.AttributeEvent.Source.CLIENT;
import static org.openremote.model.query.AssetQuery.Access;
import static org.openremote.model.query.AssetQuery.Select.selectExcludeAll;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.value.MetaItemType.*;

public class AssetResourceImpl extends ManagerWebResource implements AssetResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());

    protected final static Asset<?>[] EMPTY_ASSETS = new Asset<?>[0];
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
    public Asset<?>[] getCurrentUserAssets(RequestParams requestParams) {
        try {
            if (isSuperUser()) {
                return new Asset<?>[0];
            }

            AssetQuery assetQuery = new AssetQuery()
                .select(AssetQuery.Select.selectExcludePathAndParentInfo());

            if (!isRestrictedUser()) {
                assetQuery
                    .tenant(new TenantPredicate(getAuthenticatedRealm()))
                    .recursive(true);
            } else {
                assetQuery
                    .userIds(getUserId());
            }

            List<Asset<?>> assets = assetStorageService.findAll(assetQuery);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return assets.toArray(new Asset[0]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public UserAssetLink[] getUserAssetLinks(RequestParams requestParams, String realm, String userId, String assetId) {
        try {
            realm = TextUtil.isNullOrEmpty(realm) ? getAuthenticatedRealm() : realm;

            if (realm == null)
                throw new WebApplicationException(BAD_REQUEST);

            if (!(isSuperUser() || getAuthenticatedRealm().equals(realm)))
                throw new WebApplicationException(FORBIDDEN);

            if (userId != null && !identityService.getIdentityProvider().isUserInTenant(userId, realm))
                throw new WebApplicationException(BAD_REQUEST);

            UserAssetLink[] result = assetStorageService.findUserAssetLinks(realm, userId, assetId).toArray(new UserAssetLink[0]);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result;

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }


    @Override
    public void createUserAssetLinks(RequestParams requestParams, List<UserAssetLink> userAssetLinks) {

        // Restricted users cannot create or delete links
        if (isRestrictedUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // Check all links are for the same user and realm
        String realm = userAssetLinks.get(0).getId().getRealm();
        String userId = userAssetLinks.get(0).getId().getUserId();
        String[] assetIds = new String[userAssetLinks.size()];

        IntStream.range(0, userAssetLinks.size()).forEach(i -> {
            UserAssetLink userAssetLink = userAssetLinks.get(i);
            assetIds[i] = userAssetLink.getId().getAssetId();

            if (!userAssetLink.getId().getRealm().equals(realm) || !userAssetLink.getId().getUserId().equals(userId)) {
                throw new BadRequestException("All user asset links must be for the same user");
            }
        });

        if (!isSuperUser() && !realm.equals(getAuthenticatedRealm())) {
            throw new WebApplicationException(FORBIDDEN);
        }

        if (!identityService.getIdentityProvider().isUserInTenant(userId, realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        List<Asset<?>> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludeParentInfo(true).excludePath(true).excludeAttributes(true))
                .tenant(new TenantPredicate(realm))
                .ids(assetIds)
        );

        if (assets.size() != userAssetLinks.size()) {
            throw new BadRequestException("One or more asset IDs are invalid");
        }

        try {
            assetStorageService.storeUserAssetLinks(userAssetLinks);
        } catch (Exception e) {
            throw new WebApplicationException(BAD_REQUEST);
        }
    }

    @Override
    public void deleteUserAssetLink(RequestParams requestParams, String realm, String userId, String assetId) {
        deleteUserAssetLinks(requestParams, Collections.singletonList(new UserAssetLink(realm, userId, assetId)));
    }

    @Override
    public void deleteAllUserAssetLinks(RequestParams requestParams, String realm, String userId) {
        // Restricted users cannot create or delete links
        if (isRestrictedUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // Regular users in a different realm can not delete links
        if (!isSuperUser() && !getAuthenticatedTenant().getRealm().equals(realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // User must be in the same realm as the requested realm
        if (!identityService.getIdentityProvider().isUserInTenant(userId, realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        assetStorageService.deleteUserAssetsByUserId(userId);
    }

    @Override
    public void deleteUserAssetLinks(RequestParams requestParams, List<UserAssetLink> userAssetLinks) {
        // Restricted users cannot create or delete links
        if (isRestrictedUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // Check all links are for the same user and realm
        String realm = userAssetLinks.get(0).getId().getRealm();
        String userId = userAssetLinks.get(0).getId().getUserId();

        if (userAssetLinks.stream().anyMatch(userAssetLink -> !userAssetLink.getId().getRealm().equals(realm) || !userAssetLink.getId().getUserId().equals(userId))) {
            throw new BadRequestException("All user asset links must be for the same user");
        }

        // Regular users in a different realm can not delete links
        if (!isSuperUser() && !getAuthenticatedTenant().getRealm().equals(realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // If delete count doesn't equal link count an exception will be thrown
        try {
            assetStorageService.deleteUserAssetLinks(userAssetLinks);
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to delete user asset links", e);
            throw new BadRequestException();
        }
    }

    @Override
    public Asset<?> getPartial(RequestParams requestParams, String assetId) {
        return get(requestParams, assetId, false);
    }

    @Override
    public Asset<?> get(RequestParams requestParams, String assetId) {
        return get(requestParams, assetId, true);
    }

    public Asset<?> get(RequestParams requestParams, String assetId, boolean loadComplete) {
        try {
            Asset<?> asset;

            // Check restricted
            if (isRestrictedUser()) {
                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(FORBIDDEN);
                }
                asset = assetStorageService.find(assetId, loadComplete, Access.PROTECTED);
            } else {
                asset = assetStorageService.find(assetId, loadComplete);
            }

            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);

            if (!isTenantActiveAndAccessible(asset.getRealm())) {
                LOG.info("Forbidden access for user '" + getUsername() + "': " + asset);
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
    public void update(RequestParams requestParams, String assetId, Asset<?> asset) {
        try {
            Asset<?> storageAsset = assetStorageService.find(assetId, true);

            if (storageAsset == null)
                throw new WebApplicationException(NOT_FOUND);

            // Realm of asset must be accessible
            if (!isTenantActiveAndAccessible(storageAsset.getRealm())) {
                LOG.info("Tenant not accessible by user '" + getUsername() + "', can't update: " + storageAsset);
                throw new WebApplicationException(FORBIDDEN);
            }

            if (!storageAsset.getRealm().equals(asset.getRealm())) {
                LOG.info("Cannot change asset's realm: " + storageAsset);
                throw new WebApplicationException(FORBIDDEN);
            }

            if (!storageAsset.getType().equals(asset.getType())) {
                LOG.info("Cannot change asset's type: " + storageAsset);
                throw new WebApplicationException(FORBIDDEN);
            }

            boolean isRestrictedUser = isRestrictedUser();

            // The asset that will ultimately be stored (override/ignore some values for restricted users)
            storageAsset.setVersion(asset.getVersion());

            if (!isRestrictedUser) {
                storageAsset.setName(asset.getName());
                storageAsset.setParentId(asset.getParentId());
                storageAsset.setAccessPublicRead(asset.isAccessPublicRead());
                storageAsset.setAttributes(asset.getAttributes());
            }

            // For restricted users, merge existing and updated attributes depending on write permissions
            if (isRestrictedUser) {

                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(FORBIDDEN);
                }

                // Merge updated with existing attributes
                for (Attribute<?> updatedAttribute : asset.getAttributes().values()) {

                    // Proper validation happens on merge(), here we only need the name to continue
                    String updatedAttributeName = updatedAttribute.getName();

                    // Check if attribute is present on the asset in storage
                    Optional<Attribute<?>> serverAttribute = storageAsset.getAttribute(updatedAttributeName);
                    if (serverAttribute.isPresent()) {
                        Attribute<?> existingAttribute = serverAttribute.get();

                        // If the existing attribute is not writable by restricted client, ignore it
                        if (!existingAttribute.getMetaValue(ACCESS_RESTRICTED_WRITE).orElse(false)) {
                            LOG.info("Existing attribute not writable by restricted client, ignoring update of: " + updatedAttributeName);
                            continue;
                        }

                        // Merge updated with existing meta items (modifying a copy)
                        MetaMap updatedMetaItems = updatedAttribute.getMeta();
                        // Ensure access meta is not modified
                        updatedMetaItems.removeIf(mi -> {
                            if (mi.getName().equals(ACCESS_RESTRICTED_READ.getName())) {
                                return true;
                            }
                            if (mi.getName().equals(ACCESS_RESTRICTED_WRITE.getName())) {
                                return true;
                            }
                            if (mi.getName().equals(ACCESS_PUBLIC_READ.getName())) {
                                return true;
                            }
                            if (mi.getName().equals(ACCESS_PUBLIC_WRITE.getName())) {
                                return true;
                            }
                            return false;
                        });

                        MetaMap existingMetaItems = ValueUtil.clone(existingAttribute.getMeta());

                        existingMetaItems.addOrReplace(updatedMetaItems);

                        // Replace existing with updated attribute
                        updatedAttribute.setMeta(existingMetaItems);
                        storageAsset.getAttributes().addOrReplace(updatedAttribute);

                    } else {

                        // An attribute added by a restricted user must be readable by restricted users
                        updatedAttribute.addOrReplaceMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true));

                        // An attribute added by a restricted user must be writable by restricted users
                        updatedAttribute.addOrReplaceMeta(new MetaItem<>(ACCESS_RESTRICTED_WRITE, true));

                        // Add the new attribute
                        storageAsset.getAttributes().addOrReplace(updatedAttribute);
                    }
                }

                // Remove missing attributes
                storageAsset.getAttributes().removeIf(existingAttribute ->
                        !asset.hasAttribute(existingAttribute.getName()) && existingAttribute.getMetaValue(ACCESS_RESTRICTED_WRITE).orElse(false)
                );
            }

//            // If attribute is type RULES_TEMPLATE_FILTER, enforce meta item RULE_STATE
//            // TODO Only done for update(Asset) and not create(Asset) as we don't need that right now
//            // TODO Implement "Saved Filter/Searches" properly, allowing restricted users to create rule state flags is not great
//            resultAsset .getAttributes().stream().forEach(attribute -> {
//                if (attribute.getType().map(attributeType -> attributeType == ValueType.RULES_TEMPLATE_FILTER).orElse(false)
//                    && !attribute.hasMetaItem(MetaItemType.RULE_STATE)) {
//                    attribute.addMeta(new MetaItem<>(MetaItemType.RULE_STATE, true));
//                }
//            });

            // Store the result
            assetStorageService.merge(storageAsset, isRestrictedUser ? getUsername() : null);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, FORBIDDEN);
        } catch (ConstraintViolationException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        } catch (OptimisticLockException opEx) {
            throw new WebApplicationException("Refresh the asset from the server and try to update the changes again", opEx, CONFLICT);
        }
    }

    @Override
    public Response writeAttributeValue(RequestParams requestParams, String assetId, String attributeName, Object value) {

        Response.Status status = Response.Status.OK;

        if (value instanceof NullNode) {
            value = null;
        }

        // Process asynchronously but block for a little while waiting for the result
        Map<String, Object> headers = new HashMap<>();
        headers.put(AttributeEvent.HEADER_SOURCE, CLIENT);

        if (isAuthenticated()) {
            headers.put(Constants.AUTH_CONTEXT, getAuthContext());
        }

        AttributeWriteResult result = doAttributeWrite(new AttributeRef(assetId, attributeName), value, headers);

        if (result.getFailure() != null) {
            switch (result.getFailure()) {
                case ILLEGAL_SOURCE:
                case NO_AUTH_CONTEXT:
                case INSUFFICIENT_ACCESS:
                    status = FORBIDDEN;
                    break;
                case ASSET_NOT_FOUND:
                case ATTRIBUTE_NOT_FOUND:
                    status = NOT_FOUND;
                    break;
                case INVALID_AGENT_LINK:
                case ILLEGAL_AGENT_UPDATE:
                case INVALID_ATTRIBUTE_EXECUTE_STATUS:
                case INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE:
                    status = NOT_ACCEPTABLE;
                    break;
                default:
                    status = BAD_REQUEST;
            }
        }

        return Response.status(status).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public AttributeWriteResult[] writeAttributeValues(RequestParams requestParams, AttributeState[] attributeStates) {

        // Process asynchronously but block for a little while waiting for the result
        Map<String, Object> headers = new HashMap<>();
        headers.put(AttributeEvent.HEADER_SOURCE, CLIENT);

        if (isAuthenticated()) {
            headers.put(Constants.AUTH_CONTEXT, getAuthContext());
        }

        return Arrays.stream(attributeStates).map(attributeState ->
             doAttributeWrite(attributeState.getRef(), attributeState.getValue().orElse(null), headers)
        ).toArray(AttributeWriteResult[]::new);
    }

    @Override
    public Asset<?> create(RequestParams requestParams, Asset<?> asset) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(FORBIDDEN);
            }

            if (asset == null) {
                LOG.finer("No asset in request");
                throw new WebApplicationException(BAD_REQUEST);
            }

            // If there was no realm provided (create was called by regular user in manager UI), use the auth realm
            if (asset.getRealm() == null || asset.getRealm().length() == 0) {
                asset.setRealm(getAuthenticatedTenant().getRealm());
            } else if (!isTenantActiveAndAccessible(asset.getRealm())) {
                LOG.info("Forbidden access for user '" + getUsername() + "', can't create: " + asset);
                throw new WebApplicationException(FORBIDDEN);
            }

            Asset<?> newAsset = ValueUtil.clone(asset);

            // Allow client to set identifier
            if (asset.getId() != null) {
                newAsset.setId(asset.getId());
            }

            // TODO: Decide on the below - clients should ensure the asset conforms to the asset descriptor and we shouldn't do any 'magic' here
//            AssetModelUtil.getAssetDescriptor(asset.getType()).ifPresent(assetDescriptor -> {
//
//                // Add meta items to well known attributes if not present
//                newAsset.getAttributes().stream().forEach(assetAttribute -> {
//                    if (assetDescriptor.getAttributeDescriptors() != null) {
//                        Arrays.stream(assetDescriptor.getAttributeDescriptors())
//                                .filter(attrDescriptor -> attrDescriptor.getAttributeName().equals(assetAttribute.getName()))
//                                .findFirst()
//                                .ifPresent(defaultAttribute -> {
//                                    if (defaultAttribute.getMetaItemDescriptors() != null) {
//                                        assetAttribute.addMeta(
//                                                Arrays.stream(defaultAttribute.getMetaItemDescriptors())
//                                                        .filter(metaItemDescriptor -> !assetAttribute.hasMetaItem(metaItemDescriptor))
//                                                        .map(MetaItem::new)
//                                                        .toArray(MetaItem[]::new)
//                                        );
//                                    }
//                                });
//                    }
//                });
//
//                // Add attributes for this well known asset if not present
//                if (assetDescriptor.getAttributeDescriptors() != null) {
//                    newAsset.addAttributes(
//                            Arrays.stream(assetDescriptor.getAttributeDescriptors()).filter(attributeDescriptor ->
//                                    !newAsset.hasAttribute(attributeDescriptor.getAttributeName())).map(Attribute::new).toArray(Attribute[]::new)
//                    );
//                }
//            });

            return assetStorageService.merge(newAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public void delete(RequestParams requestParams, List<String> assetIds) {
        try {
            if (assetIds == null || assetIds.isEmpty()) {
                throw new WebApplicationException(BAD_REQUEST);
            }

            if (isRestrictedUser()) {
                throw new WebApplicationException(FORBIDDEN);
            }

            List<Asset<?>> assets = assetStorageService.findAll(new AssetQuery().ids(assetIds.toArray(new String[0])).select(selectExcludeAll()));
            if (assets == null || assets.size() != assetIds.size()) {
                LOG.fine("Request to delete one or more invalid assets");
                throw new WebApplicationException(BAD_REQUEST);
            }

            if (assets.stream().map(Asset::getRealm).distinct().anyMatch(asset -> !isTenantActiveAndAccessible(asset))) {
                LOG.info("Forbidden access for user '" + getUsername() + "', can't delete requested assets");
                throw new WebApplicationException(FORBIDDEN);
            }

            if (!assetStorageService.delete(assetIds, false)) {
                throw new WebApplicationException(BAD_REQUEST);
            }
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset<?>[] queryAssets(RequestParams requestParams, AssetQuery query) {
        try {
            if (query == null) {
                return EMPTY_ASSETS;
            }

            if (isRestrictedUser()) {
                // A restricted user can only query linked assets
                query = query.userIds(getUserId());

                // A restricted user may not query private asset data, only restricted or public
                if (query.access == null || query.access == Access.PRIVATE)
                    query.access(Access.PROTECTED);
            }

            String realm = query.tenant != null && !isNullOrEmpty(query.tenant.realm)
                ? query.tenant.realm
                : getAuthenticatedRealm();

            if (TextUtil.isNullOrEmpty(realm)) {
                throw new WebApplicationException(NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(realm)) {
                return EMPTY_ASSETS;
            }

            // This replicates behaviour of old getRoot and getChildren methods
            if (!isSuperUser()) {
                query.tenant(new TenantPredicate(realm));
            }

            List<Asset<?>> result = assetStorageService.findAll(query);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result.toArray(new Asset[0]);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset<?>[] queryPublicAssets(RequestParams requestParams, AssetQuery query) {

        String requestRealm = getRequestRealm();

        if (TextUtil.isNullOrEmpty(requestRealm)) {
            return EMPTY_ASSETS;
        }

        if (query == null) {
            query = new AssetQuery();
        }

        // Force realm to be request realm
        if (query.tenant == null) {
            query.tenant(new TenantPredicate(requestRealm));
        } else {
            query.tenant.realm = requestRealm;
        }

        // Force public access filter on query
        if (query.access == null) {
            query.access(Access.PUBLIC);
        } else {
            query.access(Access.PUBLIC);
        }

        try {
            List<Asset<?>> result = assetStorageService.findAll(query);

            // Compress response (the request attribute enables the interceptor)
            request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

            return result.toArray(new Asset[0]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Asset<?>[] getPublicAssets(RequestParams requestParams, String q) {
        AssetQuery assetQuery = TextUtil.isNullOrEmpty(q) ? null : ValueUtil.parse(q, AssetQuery.class)
            .orElseThrow(() -> new WebApplicationException("Error parsing query parameter 'q' as JSON object", BAD_REQUEST));

        Asset<?>[] result = queryPublicAssets(requestParams, assetQuery);

        // Compress response (the request attribute enables the interceptor)
        request.setAttribute(HttpHeaders.CONTENT_ENCODING, "gzip");

        return result;
    }

    protected AttributeWriteResult doAttributeWrite(AttributeRef ref, Object value, Map<String, Object> headers) {
        AttributeWriteFailure failure = null;

        try {
            AttributeEvent event = new AttributeEvent(
                ref, value, timerService.getCurrentTimeMillis()
            );

            LOG.info("Write attribute value request: " + event);

            // Process synchronously
            Object result = messageBrokerService.getProducerTemplate().requestBodyAndHeaders(
                AssetProcessingService.ASSET_QUEUE, event, headers
            );

            if (result instanceof AssetProcessingException) {
                AssetProcessingException processingException = (AssetProcessingException) result;
                failure = processingException.getReason();
            }

        } catch (AssetProcessingException e) {
            failure = e.getReason();
        } catch (IllegalStateException ex) {
            failure = AttributeWriteFailure.UNKNOWN;
        }

        return new AttributeWriteResult(ref, failure);
    }
}
