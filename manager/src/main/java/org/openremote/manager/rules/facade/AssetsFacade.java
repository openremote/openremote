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
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.rules.*;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not use assets in global scope).
 */
public class AssetsFacade<T extends Ruleset> extends Assets {

    private static final Logger LOG = Logger.getLogger(AssetsFacade.class.getName());


    public class AssetsRestrictedQueryFacade extends Assets.RestrictedQuery {

        public AssetsRestrictedQueryFacade() {
            if (TenantRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                tenant = new TenantPredicate(
                    rulesEngineId.getRealm().orElseThrow(() -> new IllegalArgumentException("Realm ID missing: " + rulesEngineId))
                );
            }
            if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                Asset restrictedAsset = assetStorageService.find(
                    rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ID missing: " + rulesEngineId)),
                    true);

                if (restrictedAsset == null) {
                    throw new IllegalStateException("Asset is no longer available: " + rulesEngineId);
                }
                path = new PathPredicate(restrictedAsset.getPath());
            }
        }

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
            if (GlobalRuleset.class.isAssignableFrom(rulesEngineId.getScope()))
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
        public Asset getResult() {
            // TODO: 'Security' checks must be done here as AssetQuery fields can easily be set by a rule
            return assetStorageService.find(this);
        }

        @Override
        public Stream<Asset> stream() {
            // TODO: 'Security' checks must be done here as AssetQuery fields can easily be set by a rule

            if (this.select == null)
                this.select = new Select();
            Include oldValue = this.select.include;
            this.select.include = Include.ONLY_ID_AND_NAME;
            try {
                return assetStorageService.findAll(this).stream();
            } finally {
                this.select.include = oldValue;
            }
        }
    }

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetStorageService assetStorageService;
    protected final Consumer<AttributeEvent> eventConsumer;

    public AssetsFacade(RulesEngineId<T> rulesEngineId, AssetStorageService assetStorageService, Consumer<AttributeEvent> eventConsumer) {
        this.rulesEngineId = rulesEngineId;
        this.assetStorageService = assetStorageService;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public Assets.RestrictedQuery query() {
        return new AssetsRestrictedQueryFacade();
    }

    @Override
    public Assets dispatch(AttributeEvent... events) {
        if (events == null)
            return this;
        for (AttributeEvent event : events) {
            // Check if the asset ID of the event can be found with the default query of this facade
            BaseAssetQuery<RestrictedQuery> checkQuery = query();
            checkQuery.ids = Collections.singletonList(event.getEntityId()); // Set directly on field, as modifying query restrictions is not allowed
            if (assetStorageService.find(checkQuery) == null) {
                LOG.warning("Access to asset not allowed for this rule engine scope " + rulesEngineId + " for event: " + event);
                continue;
            }
            eventConsumer.accept(event);
        }
        return this;
    }

    @Override
    public Assets dispatch(String assetId, String attributeName, Value value) {
        return dispatch(new AttributeEvent(assetId, attributeName, value));
    }

    @Override
    public Assets dispatch(String assetId, String attributeName, String value) {
        return dispatch(assetId, attributeName, Values.create(value));
    }

    @Override
    public Assets dispatch(String assetId, String attributeName, double value) {
        return dispatch(assetId, attributeName, Values.create(value));
    }

    @Override
    public Assets dispatch(String assetId, String attributeName, boolean value) {
        return dispatch(assetId, attributeName, Values.create(value));
    }

    @Override
    public Assets dispatch(String assetId, String attributeName, AttributeExecuteStatus status) {
        return dispatch(assetId, attributeName, status.asValue());
    }

    @Override
    public Assets dispatch(String assetId, String attributeName) {
        return dispatch(new AttributeEvent(assetId, attributeName));
    }
}
