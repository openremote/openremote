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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.CustomAssetTypeDefinition;

import java.util.List;

public class CustomAssetTypeStorageService implements ContainerService {

    protected PersistenceService persistenceService;
    protected AssetModelService assetModelService;
    protected CustomAssetTypeDefinitionValidator definitionValidator = new CustomAssetTypeDefinitionValidator();

    @Override
    public int getPriority() {
        return PersistenceService.PRIORITY + 20;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        assetModelService = container.hasService(AssetModelService.class)
            ? container.getService(AssetModelService.class)
            : null;
        container.getService(ManagerWebService.class).addApiSingleton(
            new CustomAssetTypeResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                this
            )
        );
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
        return persist(definition, false);
    }

    public CustomAssetTypeDefinition persist(CustomAssetTypeDefinition definition, boolean confirmExistingAssets) {
        CustomAssetTypeDefinition persistedDefinition = persistenceService.doReturningTransaction(em -> {
            definitionValidator.validateForCreate(definition);
            if (em.find(CustomAssetTypeDefinition.class, definition.getName()) != null) {
                throw new IllegalArgumentException("Custom asset type already exists: " + definition.getName());
            }
            if (!confirmExistingAssets && getUsageCount(em, definition.getName()) > 0) {
                throw new IllegalStateException(
                    "Existing fallback assets use this custom asset type name; confirmation is required: "
                        + definition.getName()
                );
            }
            em.persist(definition);
            return definition;
        });
        refreshAssetModel();
        return persistedDefinition;
    }

    public CustomAssetTypeDefinition merge(CustomAssetTypeDefinition definition) {
        CustomAssetTypeDefinition mergedDefinition = persistenceService.doReturningTransaction(em -> {
            CustomAssetTypeDefinition existingDefinition = em.find(CustomAssetTypeDefinition.class, definition.getName());
            definitionValidator.validateForUpdate(definition, existingDefinition, getUsageCount(em, definition.getName()));
            if (existingDefinition == null) {
                throw new IllegalArgumentException("Custom asset type does not exist: " + definition.getName());
            }
            existingDefinition
                .setDisplayName(definition.getDisplayName())
                .setIcon(definition.getIcon())
                .setColour(definition.getColour())
                .setDescription(definition.getDescription())
                .setEnabled(definition.isEnabled())
                .setAttributes(definition.getAttributes());
            return existingDefinition;
        });
        refreshAssetModel();
        return mergedDefinition;
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

    public void validate(CustomAssetTypeDefinition definition) {
        persistenceService.doTransaction(em -> {
            if (definition == null) {
                definitionValidator.validateForCreate(null);
                return;
            }
            CustomAssetTypeDefinition existingDefinition = em.find(CustomAssetTypeDefinition.class, definition.getName());
            if (existingDefinition == null) {
                definitionValidator.validateForCreate(definition);
            } else {
                definitionValidator.validateForUpdate(definition, existingDefinition, getUsageCount(em, definition.getName()));
            }
        });
    }

    public void delete(String name) {
        persistenceService.doTransaction(em -> {
            if (getUsageCount(em, name) > 0) {
                throw new IllegalStateException("Cannot delete custom asset type while assets exist: " + name);
            }
            CustomAssetTypeDefinition definition = em.find(CustomAssetTypeDefinition.class, name);
            if (definition != null) {
                em.remove(definition);
            }
        });
        refreshAssetModel();
    }

    public long getUsageCount(String typeName) {
        return persistenceService.doReturningTransaction(em -> getUsageCount(em, typeName));
    }

    protected long getUsageCount(EntityManager em, String typeName) {
        TypedQuery<Long> query = em.createQuery(
            "select count(asset.id) from Asset asset where asset.type = :typeName",
            Long.class
        );
        query.setParameter("typeName", typeName);
        return query.getSingleResult();
    }

    protected void refreshAssetModel() {
        if (assetModelService != null) {
            assetModelService.refreshDynamicModel();
        }
    }
}
