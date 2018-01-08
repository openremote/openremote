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
package org.openremote.manager.rules.facade;

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.ServerAsset;
import org.openremote.model.asset.BaseAssetQuery;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.rules.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.openremote.model.asset.AssetQuery.PathPredicate;
import static org.openremote.model.asset.AssetQuery.TenantPredicate;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not use assets in global scope).
 */
public class AssetsFacade<T extends Ruleset> extends Assets {

    protected final Class<T> rulesetType;
    protected final String rulesetId;
    protected final AssetStorageService assetStorageService;
    protected final Consumer<AttributeEvent> eventConsumer;

    public AssetsFacade(Class<T> rulesetType, String rulesetId, AssetStorageService assetStorageService, Consumer<AttributeEvent> eventConsumer) {
        this.rulesetType = rulesetType;
        this.rulesetId = rulesetId;
        this.assetStorageService = assetStorageService;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public Assets.RestrictedQuery query() {
        Assets.RestrictedQuery query = new Assets.RestrictedQuery() {

            @Override
            public Assets.RestrictedQuery select(Select select) {
                throw new IllegalArgumentException("Overriding query projection is not allowed in this rules scope");
            }

            @Override
            public Assets.RestrictedQuery id(String id) {
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public Assets.RestrictedQuery tenant(TenantPredicate tenantPredicate) {
                if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                    return super.tenant(tenantPredicate);
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public Assets.RestrictedQuery userId(String userId) {
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public Assets.RestrictedQuery orderBy(OrderBy orderBy) {
                throw new IllegalArgumentException("Overriding query result order is not allowed in this rules scope");
            }

            @Override
            public String getResult() {
                ServerAsset asset = assetStorageService.find(this);
                return asset != null ? asset.getId() : null;
            }

            @Override
            public List<String> getResults() {
                if (this.select == null)
                    this.select = new Select();
                Include oldValue = this.select.include;
                this.select.include = Include.ONLY_ID_AND_NAME;
                try {
                    return assetStorageService
                        .findAll(this)
                        .stream()
                        .map(Asset::getId)
                        .collect(Collectors.toList());
                } finally {
                    this.select.include = oldValue;
                }
            }

            @Override
            public void applyResult(Consumer<String> assetIdConsumer) {
                assetIdConsumer.accept(getResult());
            }

            @Override
            public void applyResults(Consumer<List<String>> assetIdListConsumer) {
                assetIdListConsumer.accept(getResults());
            }
        };

        if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
            query.tenantPredicate = new TenantPredicate(rulesetId);
        }
        if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
            ServerAsset restrictedAsset = assetStorageService.find(rulesetId, true);
            if (restrictedAsset == null) {
                throw new IllegalStateException("Asset is no longer available for this deployment: " + rulesetId);
            }
            query.pathPredicate = new PathPredicate(restrictedAsset.getPath());
        }
        return query;
    }

    @Override
    public void dispatch(AttributeEvent... events) {
        if (events == null)
            return;
        for (AttributeEvent event : events) {

            // Check if the asset ID of the event can be found in the original query
            BaseAssetQuery<RestrictedQuery> checkQuery = query();
            checkQuery.id = event.getEntityId();
            if (assetStorageService.find(checkQuery) == null) {
                throw new IllegalArgumentException(
                    "Access to asset not allowed for this rule engine scope: " + event
                );
            }
            eventConsumer.accept(event);
        }
    }
}
