/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.rules.facade;

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;

public class FacadeHelper {

  public static boolean doesRuleEngineScopeAllowAccess(
      RulesEngineId<? extends Ruleset> rulesEngineId,
      String assetId,
      AssetStorageService assetStorageService) {
    AssetQuery assetQuery = new AssetQuery();
    assetQuery.ids(assetId);

    if (RealmRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
      // Realm is restricted to rules
      assetQuery.realm =
          new RealmPredicate(
              rulesEngineId
                  .getRealm()
                  .orElseThrow(
                      () -> new IllegalArgumentException("Realm missing: " + rulesEngineId)));
    } else if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
      // Realm is restricted to assets'
      assetQuery.realm =
          new RealmPredicate(
              rulesEngineId
                  .getRealm()
                  .orElseThrow(
                      () -> new IllegalArgumentException("Realm missing: " + rulesEngineId)));

      Asset<?> restrictedAsset =
          assetStorageService.find(
              rulesEngineId
                  .getAssetId()
                  .orElseThrow(
                      () -> new IllegalStateException("Asset ID missing: " + rulesEngineId)),
              true);

      if (restrictedAsset == null) {
        throw new IllegalStateException("Asset is no longer available: " + rulesEngineId);
      }
      assetQuery.paths(new PathPredicate(restrictedAsset.getPath()));
    }

    assetQuery.select = new AssetQuery.Select().excludeAttributes();

    return assetStorageService.find(assetQuery) != null;
  }
}
