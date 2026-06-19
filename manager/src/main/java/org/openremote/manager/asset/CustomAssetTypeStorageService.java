/*
 * Copyright 2026 OpenRemote Inc.
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

import jakarta.persistence.TypedQuery;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.CustomAssetTypeDefinition;

import java.util.List;

public class CustomAssetTypeStorageService implements ContainerService {

    protected PersistenceService persistenceService;

    @Override
    public int getPriority() {
        return PersistenceService.PRIORITY + 20;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        // No startup work required.
    }

    @Override
    public void stop(Container container) throws Exception {
        // No shutdown work required.
    }

    public CustomAssetTypeDefinition persist(CustomAssetTypeDefinition definition) {
        return persistenceService.doReturningTransaction(em -> {
            em.persist(definition);
            return definition;
        });
    }

    public CustomAssetTypeDefinition merge(CustomAssetTypeDefinition definition) {
        return persistenceService.doReturningTransaction(em -> em.merge(definition));
    }

    public CustomAssetTypeDefinition find(String name) {
        return persistenceService.doReturningTransaction(em -> em.find(CustomAssetTypeDefinition.class, name));
    }

    public List<CustomAssetTypeDefinition> findAll() {
        return persistenceService.doReturningTransaction(em ->
            em.createQuery(
                    "select definition from CustomAssetTypeDefinition definition order by definition.name",
                    CustomAssetTypeDefinition.class
                )
                .getResultList()
        );
    }

    public void delete(String name) {
        persistenceService.doTransaction(em -> {
            CustomAssetTypeDefinition definition = em.find(CustomAssetTypeDefinition.class, name);
            if (definition != null) {
                em.remove(definition);
            }
        });
    }

    public long getUsageCount(String typeName) {
        return persistenceService.doReturningTransaction(em -> {
            TypedQuery<Long> query = em.createQuery(
                "select count(asset.id) from Asset asset where asset.type = :typeName",
                Long.class
            );
            query.setParameter("typeName", typeName);
            return query.getSingleResult();
        });
    }
}
