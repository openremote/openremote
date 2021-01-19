/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.model.query;

import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.rules.Ruleset;

import java.util.Arrays;

public class RulesetQuery {

    public long[] ids;
    public NameValuePredicate[] meta;
    public int limit;
    public Ruleset.Lang[] languages;
    public boolean fullyPopulate;
    public boolean publicOnly;
    public boolean enabledOnly;
    public String realm;
    public String[] assetIds;

    public RulesetQuery() {
    }

    public RulesetQuery setIds(long...ids) {
        this.ids = ids;
        return this;
    }

    public RulesetQuery setMeta(NameValuePredicate...meta) {
        this.meta = meta;
        return this;
    }

    public RulesetQuery setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public RulesetQuery setLanguages(Ruleset.Lang...languages) {
        this.languages = languages;
        return this;
    }

    public RulesetQuery setFullyPopulate(boolean fullyPopulate) {
        this.fullyPopulate = fullyPopulate;
        return this;
    }

    public RulesetQuery setPublicOnly(boolean publicOnly) {
        this.publicOnly = publicOnly;
        return this;
    }

    public RulesetQuery setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public RulesetQuery setAssetIds(String...assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public RulesetQuery setEnabledOnly(boolean enabledOnly) {
        this.enabledOnly = enabledOnly;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "ids=" + Arrays.toString(ids) +
            ", meta=" + Arrays.toString(meta) +
            ", limit=" + limit +
            ", languages=" + Arrays.toString(languages) +
            ", fullyPopulate=" + fullyPopulate +
            ", publicOnly=" + publicOnly +
            ", realm='" + realm + '\'' +
            ", assetIds=" + Arrays.toString(assetIds) +
            '}';
    }
}
