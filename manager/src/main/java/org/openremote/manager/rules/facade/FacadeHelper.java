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

  public static boolean doesRuleEngineScopeAllowAccess(RulesEngineId<? extends Ruleset> rulesEngineId, String assetId, AssetStorageService assetStorageService) {
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
