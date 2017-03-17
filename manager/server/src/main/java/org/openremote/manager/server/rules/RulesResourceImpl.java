/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.server.rules;

import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.rules.AssetRulesDefinition;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;
import org.openremote.manager.shared.rules.TenantRulesDefinition;
import org.openremote.model.asset.Asset;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class RulesResourceImpl extends ManagerWebResource implements RulesResource {

    final protected RulesStorageService rulesStorageService;
    final protected AssetStorageService assetStorageService;

    public RulesResourceImpl(ManagerIdentityService identityService,
                             RulesStorageService rulesStorageService,
                             AssetStorageService assetStorageService) {
        super(identityService);
        this.rulesStorageService = rulesStorageService;
        this.assetStorageService = assetStorageService;
    }

    /* ################################################################################################# */

    @Override
    public GlobalRulesDefinition[] getGlobalDefinitions(@BeanParam RequestParams requestParams) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        List<GlobalRulesDefinition> result = rulesStorageService.findGlobalDefinitions();
        return result.toArray(new GlobalRulesDefinition[result.size()]);
    }

    @Override
    public TenantRulesDefinition[] getTenantDefinitions(@BeanParam RequestParams requestParams, String realmId) {
        if (!isRealmAccessibleByUser(realmId) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        List<TenantRulesDefinition> result = rulesStorageService.findTenantDefinitions(realmId);
        return result.toArray(new TenantRulesDefinition[result.size()]);
    }

    @Override
    public AssetRulesDefinition[] getAssetDefinitions(@BeanParam RequestParams requestParams, String assetId) {
        Asset asset = assetStorageService.find(assetId);
        if (asset == null)
            return new AssetRulesDefinition[0];

        String realm = identityService.getActiveTenantRealm(asset.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() &&
            !assetStorageService.findProtectedOfUserContains(getUserId(), assetId)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        List<AssetRulesDefinition> result = rulesStorageService.findAssetDefinitions(asset.getRealmId(), assetId);
        return result.toArray(new AssetRulesDefinition[result.size()]);
    }

    /* ################################################################################################# */

    @Override
    public void createGlobalDefinition(@BeanParam RequestParams requestParams, GlobalRulesDefinition rulesDefinition) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public GlobalRulesDefinition getGlobalDefinition(@BeanParam RequestParams requestParams, Long id) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        GlobalRulesDefinition existingDefinition = rulesStorageService.findById(GlobalRulesDefinition.class, id);
        if (existingDefinition == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        return existingDefinition;
    }

    @Override
    public void updateGlobalDefinition(@BeanParam RequestParams requestParams, Long id, GlobalRulesDefinition rulesDefinition) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        GlobalRulesDefinition existingDefinition = rulesStorageService.findById(GlobalRulesDefinition.class, id);
        if (existingDefinition == null)
            throw new WebApplicationException(NOT_FOUND);
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public void deleteGlobalDefinition(@BeanParam RequestParams requestParams, Long id) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesStorageService.delete(GlobalRulesDefinition.class, id);
    }

    /* ################################################################################################# */

    @Override
    public void createTenantDefinition(@BeanParam RequestParams requestParams, TenantRulesDefinition rulesDefinition) {
        String realm = identityService.getActiveTenantRealm(rulesDefinition.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public TenantRulesDefinition getTenantDefinition(@BeanParam RequestParams requestParams, Long id) {
        TenantRulesDefinition existingDefinition = rulesStorageService.findById(TenantRulesDefinition.class, id);
        if (existingDefinition == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(existingDefinition.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return existingDefinition;
    }

    @Override
    public void updateTenantDefinition(@BeanParam RequestParams requestParams, Long id, TenantRulesDefinition rulesDefinition) {
        TenantRulesDefinition existingDefinition = rulesStorageService.findById(TenantRulesDefinition.class, id);
        if (existingDefinition == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(existingDefinition.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (!id.equals(rulesDefinition.getId())) {
            throw new WebApplicationException("Requested ID and definition ID don't match", BAD_REQUEST);
        }
        if (!existingDefinition.getRealmId().equals(rulesDefinition.getRealmId())) {
            throw new WebApplicationException("Requested realm and existing definition realm must match", BAD_REQUEST);
        }
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public void deleteTenantDefinition(@BeanParam RequestParams requestParams, Long id) {
        TenantRulesDefinition existingDefinition = rulesStorageService.findById(TenantRulesDefinition.class, id);
        if (existingDefinition == null) {
            return;
        }
        String realm = identityService.getActiveTenantRealm(existingDefinition.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesStorageService.delete(TenantRulesDefinition.class, id);
    }

    /* ################################################################################################# */

    @Override
    public void createAssetDefinition(@BeanParam RequestParams requestParams, AssetRulesDefinition rulesDefinition) {
        String assetId = rulesDefinition.getAssetId();
        if (assetId == null || assetId.length() == 0) {
            throw new WebApplicationException("Missing asset identifier value", BAD_REQUEST);
        }
        Asset asset = assetStorageService.find(assetId);
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(asset.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() &&
            !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public AssetRulesDefinition getAssetDefinition(@BeanParam RequestParams requestParams, Long id) {
        AssetRulesDefinition existingDefinition = rulesStorageService.findById(AssetRulesDefinition.class, id);
        if (existingDefinition == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Asset asset = assetStorageService.find(existingDefinition.getAssetId());
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(asset.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() &&
            !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return existingDefinition;
    }

    @Override
    public void updateAssetDefinition(@BeanParam RequestParams requestParams, Long id, AssetRulesDefinition rulesDefinition) {
        AssetRulesDefinition existingDefinition = rulesStorageService.findById(AssetRulesDefinition.class, id);
        if (existingDefinition == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Asset asset = assetStorageService.find(existingDefinition.getAssetId());
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(asset.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() &&
            !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (!id.equals(rulesDefinition.getId())) {
            throw new WebApplicationException("Requested ID and definition ID don't match", BAD_REQUEST);
        }
        if (!existingDefinition.getAssetId().equals(rulesDefinition.getAssetId())) {
            throw new WebApplicationException("Can't update asset ID, delete and create the definition to reassign", BAD_REQUEST);
        }
        rulesDefinition = rulesStorageService.merge(rulesDefinition);
    }

    @Override
    public void deleteAssetDefinition(@BeanParam RequestParams requestParams, Long id) {
        AssetRulesDefinition existingDefinition = rulesStorageService.findById(AssetRulesDefinition.class, id);
        if (existingDefinition == null) {
            return;
        }
        Asset asset = assetStorageService.find(existingDefinition.getAssetId());
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String realm = identityService.getActiveTenantRealm(asset.getRealmId());
        if (realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() &&
            !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesStorageService.delete(AssetRulesDefinition.class, id);
    }

}
