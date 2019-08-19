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

import org.openremote.model.value.ObjectValue;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that apply to all realms and assets, for the whole system.
 */
@Entity
@Table(name = "GLOBAL_RULESET")
public class GlobalRuleset extends Ruleset {

    public static final String TYPE = "global";

    public GlobalRuleset() {
    }

    public GlobalRuleset(long id, long version, Date createdOn, Date lastModified, boolean enabled, String name, Lang lang, ObjectValue meta, boolean continueOnError, String rules) {
        super(id, version, createdOn, lastModified, name, enabled, rules, lang, meta, continueOnError);
    }

    public GlobalRuleset(String name, Lang lang, String rules) {
        super(name, rules, lang, false);
    }

    public GlobalRuleset(String name, Lang lang, String rules, ObjectValue meta) {
        super(name, rules, lang, meta, false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", lang='" + lang + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", lastModified='" + lastModified + '\'' +
            ", enabled='" + enabled + '\'' +
            ", continueOnError='" + continueOnError + '\'' +
            '}';
    }

}
