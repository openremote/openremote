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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.util.TextUtil;

import javax.persistence.TypedQuery;
import java.util.List;

public class RulesetStorageService implements ContainerService {

    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
                new RulesResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        this,
                        container.getService(AssetStorageService.class),
                        container.getService(RulesService.class)
                )
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public List<GlobalRuleset> findGlobalRulesets(boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {

        String query = "select new org.openremote.model.rules.GlobalRuleset(" +
                "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.enabled, rs.name, rs.lang, rs.meta";

        query += fullyPopulate ? ", rs.rules" : ", cast(null as string)";

        query += ") " +
                "from GlobalRuleset rs where 1=1 ";

        if (onlyEnabled) {
            query += "and rs.enabled = true ";
        }

        if (language != null) {
            query += "and rs.lang = :lang ";
        }

        query += "order by rs.createdOn asc";

        String finalQuery = query;
        return persistenceService.doReturningTransaction(entityManager -> {
                TypedQuery<GlobalRuleset> qry = entityManager.createQuery(
                        finalQuery,
                        GlobalRuleset.class
                );

                if (language != null) {
                    qry.setParameter("lang", language);
                }

                return qry.getResultList();
        });
    }

    public List<TenantRuleset> findTenantRulesets(boolean onlyPublic, boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {
        return findTenantRulesets(null, onlyPublic, onlyEnabled, language, fullyPopulate);
    }
    public List<TenantRuleset> findTenantRulesets(String realm, boolean onlyPublic, boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {

        String query = "select new org.openremote.model.rules.TenantRuleset(" +
                "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.enabled, rs.name, rs.lang, rs.meta";

        query += fullyPopulate ? ", rs.rules" : ", cast(null as string)";

        query += ", rs.realm, rs.accessPublicRead) " +
                "from TenantRuleset rs " +
                "where 1=1 ";

        boolean includeRealm = !TextUtil.isNullOrEmpty(realm);

        if (includeRealm) {
            query += "and rs.realm = :realm ";
        }

        if (onlyPublic) {
            query += "and rs.accessPublicRead = true ";
        }

        if (onlyEnabled) {
            query += "and rs.enabled = true ";
        }

        if (language != null) {
            query += "and rs.lang = :lang ";
        }

        query += "order by rs.createdOn asc";

        String finalQuery = query;
        return persistenceService.doReturningTransaction(entityManager -> {
                    TypedQuery<TenantRuleset> qry = entityManager.createQuery(
                            finalQuery,
                            TenantRuleset.class
                    );
                    if (includeRealm) {
                        qry.setParameter("realm", realm);
                    }
                    if (language != null) {
                        qry.setParameter("lang", language);
                    }

                    return qry.getResultList();
                }
        );
    }

    public List<AssetRuleset> findAssetRulesetsByRealm(String realm, boolean onlyPublic, boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {
        return findAssetRulesets(realm, null, onlyPublic, onlyEnabled, language, fullyPopulate);
    }
    public List<AssetRuleset> findAssetRulesetsByAssetId(String assetId, boolean onlyPublic, boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {
        return findAssetRulesets(null, assetId, onlyPublic, onlyEnabled, language, fullyPopulate);
    }
    public List<AssetRuleset> findAssetRulesets(String realm, String assetId, boolean onlyPublic, boolean onlyEnabled, Ruleset.Lang language, boolean fullyPopulate) {

        String query = "select new org.openremote.model.rules.AssetRuleset(" +
                "rs.id, rs.version, rs.createdOn, rs.lastModified, rs.enabled, rs.name, rs.lang, rs.meta";

        query += fullyPopulate ? ", rs.rules" : ", cast(null as string)";

        query += ", a.realm, rs.assetId, rs.accessPublicRead) " +
                "from AssetRuleset rs, Asset a " +
                "where rs.assetId = a.id ";

        boolean includeRealm = !TextUtil.isNullOrEmpty(realm);
        boolean includeAssetId = !TextUtil.isNullOrEmpty(assetId);

        if (includeRealm) {
            query += "and a.realm = :realm ";
        }

        if (includeAssetId) {
            query += "and rs.assetId = :assetId ";
        }

        if (onlyPublic) {
            query += "and rs.accessPublicRead = true ";
        }

        if (onlyEnabled) {
            query += "and rs.enabled = true ";
        }

        if (language != null) {
            query += "and rs.lang = :lang ";
        }

        query += "order by rs.createdOn asc";

        String finalQuery = query;

        return persistenceService.doReturningTransaction(entityManager -> {
                    TypedQuery<AssetRuleset> qry = entityManager.createQuery(
                            finalQuery,
                            AssetRuleset.class
                    );

                    if (includeRealm) {
                        qry.setParameter("realm", realm);
                    }
                    if (includeAssetId) {
                        qry.setParameter("assetId", assetId);
                    }
                    if (language != null) {
                        qry.setParameter("lang", language);
                    }

                    return qry.getResultList();
                }
        );
    }

    public <T extends Ruleset> T findById(Class<T> rulesetType, Long id) {

        return persistenceService.doReturningTransaction(em -> {
            T ruleset = em.find(rulesetType, id);

            if (rulesetType == AssetRuleset.class && ruleset != null && TextUtil.isNullOrEmpty(((AssetRuleset)ruleset).getRealm())) {
                // Asset rulesets require special treatment as realm is transient - TODO Should a JPA mapping be used?
                String realm = em.createQuery("select a.realm from AssetRuleset rs, Asset a where a.id = rs.assetId and rs.id = :id", String.class).setParameter("id", id).getSingleResult();
                ((AssetRuleset) ruleset).setRealm(realm);
            }

            return ruleset;
        });
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
