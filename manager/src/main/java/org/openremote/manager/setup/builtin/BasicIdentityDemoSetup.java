/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.setup.builtin;

import com.fasterxml.uuid.Generators;
import org.hibernate.Session;
import org.openremote.container.Container;
import org.openremote.container.security.basic.PasswordStorage;
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.Setup;
import org.openremote.model.security.Tenant;
import org.openremote.model.Constants;

import java.sql.PreparedStatement;

/**
 * We have the following demo users:
 * <ul>
 * <li><code>admin</code> - The superuser in the "master" realm with all access</li>
 * </ul>
 */
public class BasicIdentityDemoSetup implements Setup {

    public static final String SETUP_ADMIN_PASSWORD = "SETUP_ADMIN_PASSWORD";
    public static final String SETUP_ADMIN_PASSWORD_DEFAULT = "secret";

    final protected ManagerPersistenceService persistenceService;
    final protected ManagerIdentityService identityService;
    final protected String demoAdminPassword;

    public Tenant masterTenant;

    public BasicIdentityDemoSetup(Container container) {
        this.persistenceService = container.getService(ManagerPersistenceService.class);
        this.identityService = container.getService(ManagerIdentityService.class);

        this.demoAdminPassword = container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT);
    }

    @Override
    public void onStart() throws Exception {

        // Tenants
        masterTenant = identityService.getIdentityProvider().getTenantForRealm(Constants.MASTER_REALM);

        // Users


        persistenceService.doTransaction(em -> em.unwrap(Session.class).doWork(connection -> {
            String sql = "insert into PUBLIC.USER_ENTITY(ID, REALM_ID, USERNAME, PASSWORD, ENABLED) values (?, 'master', 'admin', ?, true)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, Generators.randomBasedGenerator().generate().toString());
            st.setString(2, PasswordStorage.createHash(demoAdminPassword));
            st.executeUpdate();
            st.close();
        }));

    }
}
