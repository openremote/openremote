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
package org.openremote.manager.rules;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.RulesetQuery;
import org.openremote.model.rules.*;
import org.openremote.model.rules.geofence.GeofenceDefinition;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Tenant;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class RulesResourceImpl extends ManagerWebResource implements RulesResource {

    private static final Logger LOG = Logger.getLogger(RulesResourceImpl.class.getName());

    final protected RulesetStorageService rulesetStorageService;
    final protected AssetStorageService assetStorageService;
    final protected RulesService rulesService;

    public RulesResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             RulesetStorageService rulesetStorageService,
                             AssetStorageService assetStorageService,
                             RulesService rulesService) {
        super(timerService, identityService);
        this.rulesetStorageService = rulesetStorageService;
        this.assetStorageService = assetStorageService;
        this.rulesService = rulesService;
    }

    /* ################################################################################################# */

    @Override
    public RulesEngineInfo getGlobalEngineInfo(RequestParams requestParams) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return getEngineInfo(rulesService.globalEngine);
    }

    @Override
    public RulesEngineInfo getTenantEngineInfo(RequestParams requestParams, String realm) {
        if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        RulesEngine<TenantRuleset> engine = rulesService.tenantEngines.get(realm);
        return getEngineInfo(engine);
    }

    @Override
    public RulesEngineInfo getAssetEngineInfo(RequestParams requestParams, String assetId) {
        Asset<?> asset = assetStorageService.find(assetId, false);

        if (asset == null)
            return null;

        if (!isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        RulesEngine<AssetRuleset> engine = rulesService.assetEngines.get(assetId);
        return getEngineInfo(engine);
    }

    protected RulesEngineInfo getEngineInfo(RulesEngine engine) {
        if (engine == null) {
            return null;
        }

        int compilationErrorCount = engine.getCompilationErrorDeploymentCount();
        int executionErrorCount = engine.getExecutionErrorDeploymentCount();

        return new RulesEngineInfo(
            engine.getStatus(),
            compilationErrorCount,
            executionErrorCount
        );
    }

    @Override
    public GlobalRuleset[] getGlobalRulesets(@BeanParam RequestParams requestParams, List<Ruleset.Lang> languages, boolean fullyPopulate) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        List<GlobalRuleset> result = rulesetStorageService.findAll(GlobalRuleset.class, new RulesetQuery().setLanguages(languages.toArray(new Ruleset.Lang[0])).setFullyPopulate(fullyPopulate));

        // Try and retrieve transient status and error data
        result.forEach(ruleset ->
            rulesService
                .getRulesetDeployment(ruleset.getId())
                .ifPresent(rulesetDeployment -> {
                    ruleset.setStatus(rulesetDeployment.getStatus());
                    ruleset.setError(rulesetDeployment.getErrorMessage());
                })
        );

        return result.toArray(new GlobalRuleset[0]);
    }

    @Override
    public TenantRuleset[] getTenantRulesets(@BeanParam RequestParams requestParams, String realm, List<Ruleset.Lang> languages, boolean fullyPopulate) {

        if (isAuthenticated() && !isRealmAccessibleByUser(realm)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        boolean publicOnly = !isAuthenticated() || isRestrictedUser() | !hasResourceRole(ClientRole.READ_RULES.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        List<TenantRuleset> result = rulesetStorageService.findAll(
            TenantRuleset.class,
            new RulesetQuery().
                setRealm(realm)
                .setLanguages(languages.toArray(new Ruleset.Lang[0]))
                .setFullyPopulate(fullyPopulate)
                .setPublicOnly(publicOnly));

        // Try and retrieve transient status and error data
        result.forEach(ruleset ->
            rulesService
                .getRulesetDeployment(ruleset.getId())
                .ifPresent(rulesetDeployment -> {
                    ruleset.setStatus(rulesetDeployment.getStatus());
                    ruleset.setError(rulesetDeployment.getErrorMessage());
                })
        );

        return result.toArray(new TenantRuleset[0]);
    }

    @Override
    public AssetRuleset[] getAssetRulesets(@BeanParam RequestParams requestParams, String assetId, List<Ruleset.Lang> languages, boolean fullyPopulate) {
        Asset<?> asset = assetStorageService.find(assetId, false);
        if (asset == null)
            return new AssetRuleset[0];

        if (isAuthenticated() && !isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        boolean publicOnly = !isAuthenticated() || (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) || !hasResourceRole(ClientRole.READ_RULES.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        List<AssetRuleset> result = rulesetStorageService.findAll(
            AssetRuleset.class,
            new RulesetQuery()
                .setRealm(asset.getRealm())
                .setAssetIds(assetId)
                .setPublicOnly(publicOnly)
                .setLanguages(languages.toArray(new Ruleset.Lang[0]))
                .setFullyPopulate(fullyPopulate));

        // Try and retrieve transient status and error data
        result.forEach(ruleset ->
            rulesService
                .getRulesetDeployment(ruleset.getId())
                .ifPresent(rulesetDeployment -> {
                    ruleset.setStatus(rulesetDeployment.getStatus());
                    ruleset.setError(rulesetDeployment.getErrorMessage());
                })
        );
        return result.toArray(new AssetRuleset[0]);
    }

    /* ################################################################################################# */

    @Override
    public long createGlobalRuleset(@BeanParam RequestParams requestParams, GlobalRuleset ruleset) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        ruleset = rulesetStorageService.merge(ruleset);
        return ruleset.getId();
    }

    @Override
    public GlobalRuleset getGlobalRuleset(@BeanParam RequestParams requestParams, Long id) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        GlobalRuleset ruleset = rulesetStorageService.find(GlobalRuleset.class, id);
        if (ruleset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        return ruleset;
    }

    @Override
    public void updateGlobalRuleset(@BeanParam RequestParams requestParams, Long id, GlobalRuleset ruleset) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        GlobalRuleset existingRuleset = rulesetStorageService.find(GlobalRuleset.class, id);
        if (existingRuleset == null)
            throw new WebApplicationException(NOT_FOUND);
        rulesetStorageService.merge(ruleset);
    }

    @Override
    public void deleteGlobalRuleset(@BeanParam RequestParams requestParams, Long id) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesetStorageService.delete(GlobalRuleset.class, id);
    }

    /* ################################################################################################# */

    @Override
    public long createTenantRuleset(@BeanParam RequestParams requestParams, TenantRuleset ruleset) {
        Tenant tenant = identityService.getIdentityProvider().getTenant(ruleset.getRealm());
        if (tenant == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isTenantActiveAndAccessible(tenant) || isRestrictedUser()) {
            LOG.info("Forbidden access for user '" + getUsername() + "': " + tenant);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        ruleset = rulesetStorageService.merge(ruleset);
        return ruleset.getId();
    }

    @Override
    public TenantRuleset getTenantRuleset(@BeanParam RequestParams requestParams, Long id) {
        TenantRuleset ruleset = rulesetStorageService.find(TenantRuleset.class, id);
        if (ruleset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Tenant tenant = identityService.getIdentityProvider().getTenant(ruleset.getRealm());
        if (tenant == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isTenantActiveAndAccessible(tenant) || isRestrictedUser()) {
            LOG.fine("Forbidden access for user '" + getUsername() + "': " + tenant);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return ruleset;
    }

    @Override
    public void updateTenantRuleset(@BeanParam RequestParams requestParams, Long id, TenantRuleset ruleset) {
        TenantRuleset existingRuleset = rulesetStorageService.find(TenantRuleset.class, id);
        if (existingRuleset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Tenant tenant = identityService.getIdentityProvider().getTenant(existingRuleset.getRealm());
        if (tenant == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isTenantActiveAndAccessible(tenant) || isRestrictedUser()) {
            LOG.fine("Forbidden access for user '" + getUsername() + "': " + tenant);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (!id.equals(ruleset.getId())) {
            throw new WebApplicationException("Requested ID and ruleset ID don't match", BAD_REQUEST);
        }
        if (!existingRuleset.getRealm().equals(ruleset.getRealm())) {
            throw new WebApplicationException("Requested realm and existing ruleset realm must match", BAD_REQUEST);
        }
        rulesetStorageService.merge(ruleset);
    }

    @Override
    public void deleteTenantRuleset(@BeanParam RequestParams requestParams, Long id) {
        TenantRuleset ruleset = rulesetStorageService.find(TenantRuleset.class, id);
        if (ruleset == null) {
            return;
        }
        Tenant tenant = identityService.getIdentityProvider().getTenant(ruleset.getRealm());
        if (tenant == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if (!isTenantActiveAndAccessible(tenant) || isRestrictedUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesetStorageService.delete(TenantRuleset.class, id);
    }

    /* ################################################################################################# */

    @Override
    public long createAssetRuleset(@BeanParam RequestParams requestParams, AssetRuleset ruleset) {
        String assetId = ruleset.getAssetId();
        if (assetId == null || assetId.length() == 0) {
            throw new WebApplicationException("Missing asset identifier value", BAD_REQUEST);
        }
        Asset<?> asset = assetStorageService.find(assetId, false);
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        if (!isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        ruleset = rulesetStorageService.merge(ruleset);
        return ruleset.getId();
    }

    @Override
    public AssetRuleset getAssetRuleset(@BeanParam RequestParams requestParams, Long id) {
        AssetRuleset ruleset = rulesetStorageService.find(AssetRuleset.class, id);
        if (ruleset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Asset<?> asset = assetStorageService.find(ruleset.getAssetId(), false);
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        if (!isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return ruleset;
    }

    @Override
    public void updateAssetRuleset(@BeanParam RequestParams requestParams, Long id, AssetRuleset ruleset) {
        AssetRuleset existingRuleset = rulesetStorageService.find(AssetRuleset.class, id);
        if (existingRuleset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Asset<?> asset = assetStorageService.find(existingRuleset.getAssetId(), false);
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        if (!isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (!id.equals(ruleset.getId())) {
            throw new WebApplicationException("Requested ID and ruleset ID don't match", BAD_REQUEST);
        }
        if (!existingRuleset.getAssetId().equals(ruleset.getAssetId())) {
            throw new WebApplicationException("Can't update asset ID, delete and create the ruleset to reassign",
                                              BAD_REQUEST);
        }
        rulesetStorageService.merge(ruleset);
    }

    @Override
    public void deleteAssetRuleset(@BeanParam RequestParams requestParams, Long id) {
        AssetRuleset ruleset = rulesetStorageService.find(AssetRuleset.class, id);
        if (ruleset == null) {
            return;
        }
        Asset<?> asset = assetStorageService.find(ruleset.getAssetId(), false);
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        if (!isRealmAccessibleByUser(asset.getRealm())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), asset.getId())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        rulesetStorageService.delete(AssetRuleset.class, id);
    }

    @Override
    public GeofenceDefinition[] getAssetGeofences(@BeanParam RequestParams requestParams, String assetId) {
        Asset<?> asset;

        asset = assetStorageService.find(
            new AssetQuery()
                .select(AssetQuery.Select.selectExcludePathAndAttributes())
                .ids(assetId));

        if (asset == null)
            return new GeofenceDefinition[0];

        // If not linked to users check if asset is marked as public read
        if (!asset.isAccessPublicRead()) {

            // If asset is linked to users then only those users can get the geofences for it
            List<UserAssetLink> userAssetLinks = assetStorageService.findUserAssetLinks(asset.getRealm(), null, assetId);

            if (!userAssetLinks.isEmpty()) {
                if (!isAuthenticated() || userAssetLinks.stream().noneMatch(userAssetLink -> userAssetLink.getId().getUserId().equals(getUserId()))) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
            }
        }

        return rulesService.getAssetGeofences(assetId);
    }

}
