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
package org.openremote.manager.server.setup.builtin;

import org.hibernate.Session;
import org.openremote.container.Container;
import org.openremote.manager.server.persistence.ManagerPersistenceService;
import org.openremote.manager.server.setup.Setup;

import java.sql.PreparedStatement;

public class BasicIdentityInitSetup implements Setup {

    final protected ManagerPersistenceService persistenceService;

    public BasicIdentityInitSetup(Container container) {
        this.persistenceService = container.getService(ManagerPersistenceService.class);
    }

    @Override
    public void onStart() {
        // Configure the master realm
        persistenceService.doTransaction(em -> em.unwrap(Session.class).doWork(connection -> {
            String sql = "insert into PUBLIC.REALM(ID, NAME, ENABLED) values ('master', 'master', true)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.executeUpdate();
            st.close();

            sql = "insert into PUBLIC.REALM_ATTRIBUTE(REALM_ID, NAME, VALUE) values ('master', 'displayName', 'Master')";
            st = connection.prepareStatement(sql);
            st.executeUpdate();
            st.close();
        }));
    }
}
