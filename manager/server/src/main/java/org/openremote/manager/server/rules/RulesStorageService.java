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
import org.openremote.manager.shared.rules.AssetRulesDefinition;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesDefinition;
import org.openremote.manager.shared.rules.TenantRulesDefinition;

import java.util.List;

public class RulesStorageService implements ContainerService {

    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new RulesResourceImpl(
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
     * The {@link RulesDefinition#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<GlobalRulesDefinition> findGlobalDefinitions() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.GlobalRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled" +
                    ") " +
                    "from GlobalRulesDefinition rd " +
                    "order by rd.createdOn asc",
                GlobalRulesDefinition.class
            ).getResultList()
        );
    }

    /**
     * The {@link RulesDefinition#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<TenantRulesDefinition> findTenantDefinitions(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.realmId" +
                    ") " +
                    "from TenantRulesDefinition rd " +
                    "where rd.realmId = :realmId " +
                    "order by rd.createdOn asc",
                TenantRulesDefinition.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    /**
     * The {@link RulesDefinition#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<AssetRulesDefinition> findAssetDefinitions(String realmId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.assetId" +
                    ") " +
                    "from AssetRulesDefinition rd, Asset a " +
                    "where rd.assetId = :assetId and rd.assetId = a.id and a.realmId = :realmId " +
                    "order by rd.createdOn asc",
                AssetRulesDefinition.class
            ).setParameter("realmId", realmId).setParameter("assetId", assetId).getResultList()
        );
    }

    /**
     * @return Fully populated rules definitions including {@link RulesDefinition#rules} property.
     */
    public List<GlobalRulesDefinition> findEnabledGlobalDefinitions() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.GlobalRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.rules" +
                    ") " +
                    "from GlobalRulesDefinition rd " +
                    "where rd.enabled = true",
                GlobalRulesDefinition.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated rules definitions including {@link RulesDefinition#rules} property.
     */
    public List<TenantRulesDefinition> findEnabledTenantDefinitions() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.rules, rd.realmId" +
                    ") " +
                    "from TenantRulesDefinition rd " +
                    "where rd.enabled = true",
                TenantRulesDefinition.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated rules definitions including {@link RulesDefinition#rules} property.
     */
    public List<TenantRulesDefinition> findEnabledTenantDefinitions(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.rules, rd.realmId" +
                    ") " +
                    "from TenantRulesDefinition rd " +
                    "where rd.enabled = true and rd.realmId = :realmId",
                TenantRulesDefinition.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    /**
     * @return Fully populated rules definitions including {@link RulesDefinition#rules} property.
     */
    public List<AssetRulesDefinition> findEnabledAssetDefinitions() {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.rules, rd.assetId, a.realmId" +
                    ") " +
                    "from AssetRulesDefinition rd, Asset a " +
                    "where rd.assetId = a.id " +
                    "and rd.enabled = true ",
                AssetRulesDefinition.class
            ).getResultList()
        );
    }

    /**
     * @return Fully populated rules definitions including {@link RulesDefinition#rules} property.
     */
    public List<AssetRulesDefinition> findEnabledAssetDefinitions(String realmId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.rules, rd.assetId, a.realmId" +
                    ") " +
                    "from AssetRulesDefinition rd, Asset a " +
                    "where rd.assetId = a.id and a.realmId = :realmId " +
                    "and rd.enabled = true",
                AssetRulesDefinition.class
            ).setParameter("realmId", realmId).getResultList()
        );
    }

    public <T extends RulesDefinition> T findById(Class<T> rulesDefinitionType, Long id) {
        return persistenceService.doReturningTransaction(em -> em.find(rulesDefinitionType, id));
    }

    public <T extends RulesDefinition> T merge(T rulesDefinition) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.merge(rulesDefinition)
        );
    }

    public <T extends RulesDefinition> void delete(Class<T> rulesDefinitionType, Long id) {
        persistenceService.doTransaction(entityManager -> {
            RulesDefinition rulesDefinition = entityManager.find(rulesDefinitionType, id);
            if (rulesDefinition != null)
                entityManager.remove(rulesDefinition);
        });
    }
}
