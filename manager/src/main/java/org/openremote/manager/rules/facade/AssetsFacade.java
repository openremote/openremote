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
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RealmRuleset;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not use assets in global scope).
 */
public class AssetsFacade<T extends Ruleset> extends Assets {

    private static final Logger LOG = Logger.getLogger(AssetsFacade.class.getName());

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetStorageService assetStorageService;
    protected final Consumer<AttributeEvent> eventConsumer;

    public AssetsFacade(RulesEngineId<T> rulesEngineId, AssetStorageService assetStorageService, Consumer<AttributeEvent> eventConsumer) {
        this.rulesEngineId = rulesEngineId;
        this.assetStorageService = assetStorageService;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public Stream<Asset<?>> getResults(AssetQuery assetQuery) {

        if (RealmRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            // Realm is restricted to rules
            assetQuery.realm = new RealmPredicate(
                rulesEngineId.getRealm().orElseThrow(() -> new IllegalArgumentException("Realm missing: " + rulesEngineId))
            );
        } else if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            // Realm is restricted to assets'
            assetQuery.realm = new RealmPredicate(
                rulesEngineId.getRealm().orElseThrow(() -> new IllegalArgumentException("Realm missing: " + rulesEngineId))
            );

            Asset<?> restrictedAsset = assetStorageService.find(
                rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ID missing: " + rulesEngineId)),
                true);

            if (restrictedAsset == null) {
                throw new IllegalStateException("Asset is no longer available: " + rulesEngineId);
            }
            assetQuery.paths(new PathPredicate(restrictedAsset.getPath()));
        }

        AssetQuery.Select oldValue = assetQuery.select;
        assetQuery.select = new AssetQuery.Select().excludeAttributes();

        try {
            return assetStorageService.findAll(assetQuery).stream();
        } finally {
            assetQuery.select = oldValue;
        }
    }

    public AssetsFacade<T> dispatch(AttributeEvent... events) {
        if (events == null || events.length == 0)
            return this;

        // Check if the asset ID of every event can be found with the default security of this facade
        String[] ids = Arrays.stream(events).map(AttributeEvent::getId).toArray(String[]::new);

        AssetQuery query = new AssetQuery().ids(ids);
        long count = this.getResults(query).count();

        if (ids.length != count) {
            LOG.warning("Access to asset(s) not allowed for this rule engine scope " + rulesEngineId + " for asset IDs: " + String.join(", ", ids));
            return this;
        }

        for (AttributeEvent event : events) {
            eventConsumer.accept(event);
        }
        return this;
    }

    public AssetsFacade<T> dispatch(String assetId, String attributeName, Object value) {
        return dispatch(new AttributeEvent(assetId, attributeName, value));
    }

    public AssetsFacade<T> dispatch(String assetId, String attributeName) {
        return dispatch(new AttributeEvent(assetId, attributeName, null));
    }
}
