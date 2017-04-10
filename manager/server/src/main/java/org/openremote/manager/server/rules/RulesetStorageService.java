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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.rules.AssetRuleset;
import org.openremote.manager.shared.rules.GlobalRuleset;
import org.openremote.manager.shared.rules.Ruleset;
import org.openremote.manager.shared.rules.TenantRuleset;

import java.util.List;

public class RulesetStorageService implements ContainerService {

    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new RulesetResourceImpl(
                container.getService(ManagerIdentityService.class),
                this,
                container.getService(AssetStorageService.class)
            )
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    /**
     * The {@link Ruleset#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<GlobalRuleset> findGlobalRulesets() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.GlobalRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled" +
                    ") " +
                    "from GlobalRuleset rs " +
                    "order by rs.createdOn asc",
                GlobalRuleset.class
            ).getResultList()
        );
    }

    /**
     * The {@link Ruleset#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<TenantRuleset> findTenantRulesets(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.realmId" +
                    ") " +
                    "from TenantRuleset rs " +
                    "where rs.realmId = :realmId " +
                    "order by rs.createdOn asc",
                TenantRuleset.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    /**
     * The {@link Ruleset#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<AssetRuleset> findAssetRulesets(String realmId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.assetId" +
                    ") " +
                    "from AssetRuleset rs, Asset a " +
                    "where rs.assetId = :assetId and rs.assetId = a.id and a.realmId = :realmId " +
                    "order by rs.createdOn asc",
                AssetRuleset.class
            ).setParameter("realmId", realmId).setParameter("assetId", assetId).getResultList()
        );
    }

    /**
     * @return Fully populated rulesets including {@link Ruleset#rules} property.
     */
    public List<GlobalRuleset> findEnabledGlobalRulesets() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.GlobalRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules" +
                    ") " +
                    "from GlobalRuleset rs " +
                    "where rs.enabled = true",
                GlobalRuleset.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated rulesets including {@link Ruleset#rules} property.
     */
    public List<TenantRuleset> findEnabledTenantRulesets() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules, rs.realmId" +
                    ") " +
                    "from TenantRuleset rs " +
                    "where rs.enabled = true",
                TenantRuleset.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated rulesets including {@link Ruleset#rules} property.
     */
    public List<TenantRuleset> findEnabledTenantRulesets(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules, rs.realmId" +
                    ") " +
                    "from TenantRuleset rs " +
                    "where rs.enabled = true and rs.realmId = :realmId",
                TenantRuleset.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    /**
     * @return Fully populated rulesets including {@link Ruleset#rules} property.
     */
    public List<AssetRuleset> findEnabledAssetRulesets() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules, rs.assetId, a.realmId" +
                    ") " +
                    "from AssetRuleset rs, Asset a " +
                    "where rs.assetId = a.id " +
                    "and rs.enabled = true ",
                AssetRuleset.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated ruleset including {@link Ruleset#rules} property.
     */
    public AssetRuleset findEnabledAssetRuleset(Long id) {
        return persistenceService.doReturningTransaction(entityManager -> {
                List<AssetRuleset> result = entityManager.createQuery(
                    "select new org.openremote.manager.shared.rules.AssetRuleset(" +
                        "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules, rs.assetId, a.realmId" +
                        ") " +
                        "from AssetRuleset rs, Asset a " +
                        "where rs.assetId = a.id " +
                        "and rs.id = :id " +
                        "and rs.enabled = true ",
                    AssetRuleset.class
                ).setParameter("id", id).getResultList();
                return result.size() > 0 ? result.get(0) : null;
            }
        );
    }

    /**
     * @return Fully populated rulesets including {@link Ruleset#rules} property.
     */
    public List<AssetRuleset> findEnabledAssetRulesets(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRuleset(" +
                    "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.name, rs.enabled, rs.rules, rs.assetId, a.realmId" +
                    ") " +
                    "from AssetRuleset rs, Asset a " +
                    "where rs.assetId = a.id and a.realmId = :realmId " +
                    "and rs.enabled = true",
                AssetRuleset.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    public <T extends Ruleset> T findById(Class<T> rulesetType, Long id) {
        return persistenceService.doReturningTransaction(em -> em.find(rulesetType, id));
    }

    public <T extends Ruleset> T merge(T ruleset) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.merge(ruleset)
        );
    }

    public <T extends Ruleset> void delete(Class<T> rulesetType, Long id) {
        persistenceService.doTransaction(entityManager -> {
            Ruleset ruleset = entityManager.find(rulesetType, id);
            if (ruleset != null)
                entityManager.remove(ruleset);
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
