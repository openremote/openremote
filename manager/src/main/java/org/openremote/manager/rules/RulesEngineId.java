/*
 * Copyright 2018, OpenRemote Inc.
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

import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.TenantRuleset;

import java.util.Objects;
import java.util.Optional;

/**
 * The scope of a {@link RulesEngine} and, optional, the tenant or asset it belongs to.
 *
 * An engine in {@link org.openremote.model.rules.GlobalRuleset} scope has no {@link #realmId}
 * or {@link #assetId}. An engine {@link org.openremote.model.rules.TenantRuleset} scope has only
 * {@link #realmId}. An engine in {@link org.openremote.model.rules.AssetRuleset} scope has both.
 */
public class RulesEngineId<T extends Ruleset> {

    final protected Class<T> scope;
    final protected String realmId;
    final protected String assetId;

    @SuppressWarnings("unchecked")
    public RulesEngineId() {
        this((Class<T>) GlobalRuleset.class, null, null);
    }

    @SuppressWarnings("unchecked")
    public RulesEngineId(String realmId) {
        this((Class<T>) TenantRuleset.class, realmId, null);
    }

    @SuppressWarnings("unchecked")
    public RulesEngineId(String realmId, String assetId) {
        this((Class<T>) AssetRuleset.class, realmId, assetId);
    }

    protected RulesEngineId(Class<T> scope, String realmId, String assetId) {
        this.scope = scope;
        this.realmId = realmId;
        this.assetId = assetId;
    }

    public Class<T> getScope() {
        return scope;
    }

    public Optional<String> getRealmId() {
        return Optional.ofNullable(realmId);
    }

    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RulesEngineId that = (RulesEngineId) o;
        return scope == that.scope &&
            Objects.equals(realmId, that.realmId) &&
            Objects.equals(assetId, that.assetId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(scope, realmId, assetId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "scope=" + scope.getSimpleName() +
            ", realmId='" + realmId + '\'' +
            ", assetId='" + assetId + '\'' +
            '}';
    }
}
