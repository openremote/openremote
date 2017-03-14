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
import java.util.logging.Logger;

public class RulesStorageService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(RulesStorageService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);

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
    public List<TenantRulesDefinition> findTenantDefinitions(String realm) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.TenantRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.realm" +
                    ") " +
                    "from TenantRulesDefinition rd " +
                    "where rd.realm = :realm " +
                    "order by rd.createdOn asc",
                TenantRulesDefinition.class
            ).setParameter("realm", realm).getResultList()
        );
    }

    /**
     * The {@link RulesDefinition#rules} property is not populated for this query to avoid
     * loading multiple large strings.
     */
    public List<AssetRulesDefinition> findAssetDefinitions(String realm, String assetId) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.createQuery(
                "select new org.openremote.manager.shared.rules.AssetRulesDefinition(" +
                    "rd.id, rd.version, rd.createdOn, rd.lastModified, rd.name, rd.enabled, rd.assetId" +
                    ") " +
                    "from AssetRulesDefinition rd, Asset a " +
                    "where rd.assetId = :assetId and rd.assetId = a.id and a.realm = :realm " +
                    "order by rd.createdOn asc",
                AssetRulesDefinition.class
            ).setParameter("realm", realm).setParameter("assetId", assetId).getResultList()
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
