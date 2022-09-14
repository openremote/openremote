/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.SecuritySettingPlugin;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;

import java.util.Map;
import java.util.Set;

public class ORSecuritySettingPlugin implements SecuritySettingPlugin {
    @Override
    public SecuritySettingPlugin init(Map<String, String> options) {
        return null;
    }

    @Override
    public SecuritySettingPlugin stop() {
        return null;
    }

    @Override
    public Map<String, Set<Role>> getSecurityRoles() {
        return Map.of(
            "master.*.asset.#", Set.of(
                new Role("readassets", false, true, true, true, true, true, false, false, true, true),
                new Role("writeassets", false, true, true, true, true, true, false, false, true, true)
            ),
            "master.*.asset.1234", Set.of(
                new Role("readasset-1234", false, true, true, true, true, true, false, false, true, true),
                new Role("readassets", false, true, true, true, true, true, false, false, true, true)
            ),
            "master.*.asset.5678", Set.of(
                new Role("readasset-5678", false, true, true, true, true, true, false, false, true, true),
                new Role("readassets", false, true, true, true, true, true, false, false, true, true)
            ),
            "master.*.attribute.#", Set.of(new Role("readassets", false, true, true, true, true, true, false, false, true, true)),
            "master.*.attribute.abcd.1234", Set.of(
                new Role("readassets", false, true, true, true, true, true, false, false, true, true),
                new Role("readasset-1234-abcd", false, true, true, true, true, true, false, false, true, true)
            ),
            "master.*.attribute.efgh.1234", Set.of(
                new Role("guests", false, true, true, true, true, true, false, false, true, true),
                new Role("readassets", false, true, true, true, true, true, false, false, true, true)
            )
        );
    }

    @Override
    public void setSecurityRepository(HierarchicalRepository<Set<Role>> securityRepository) {

    }
}
