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
package org.openremote.model.rules;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that apply to all realms and assets, for the whole system.
 */
@Entity
@Table(name = "GLOBAL_RULESET")
public class GlobalRuleset extends Ruleset {

    public GlobalRuleset() {
    }

    public GlobalRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String templateAssetId) {
        super(id, version, createdOn, lastModified, name, enabled, templateAssetId);
    }

    public GlobalRuleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String templateAssetId, String rules) {
        super(id, version, createdOn, lastModified, name, enabled, templateAssetId, rules);
    }

    public GlobalRuleset(String name, String rules) {
        super(name, rules);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }


}
