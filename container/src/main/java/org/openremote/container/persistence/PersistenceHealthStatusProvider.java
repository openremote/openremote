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
package org.openremote.container.persistence;

import com.zaxxer.hikari.HikariPoolMXBean;
import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.Database.PROPERTY_POOL_NAME;

public class PersistenceHealthStatusProvider implements HealthStatusProvider {

    private static final Logger LOG = Logger.getLogger(PersistenceHealthStatusProvider.class.getName());
    public static final String NAME = "db";
    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public String getHealthStatusName() {
        return NAME;
    }

    @Override
    public Object getHealthStatus() {
        if (persistenceService.persistenceUnitProperties == null) {
            return null;
        }

        String poolNameStr = persistenceService.persistenceUnitProperties.containsKey(PROPERTY_POOL_NAME)
            ? persistenceService.persistenceUnitProperties.get(PROPERTY_POOL_NAME).toString()
            : "or-pool";

        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolNameStr + ")");
            HikariPoolMXBean poolMBean = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);

            int idleConnections = poolMBean.getIdleConnections();
            int activeConnections = poolMBean.getActiveConnections();
            int totalConnections = poolMBean.getTotalConnections();
            int threadsWaiting = poolMBean.getThreadsAwaitingConnection();

            return Map.<String, Object>of(
                "idleConnections", idleConnections,
                "activeConnections", activeConnections,
                "totalConnections", totalConnections,
                "threadsWaiting", threadsWaiting);
        } catch (MalformedObjectNameException e) {
            LOG.log(Level.SEVERE, "Failed to get hikari connection pool status", e);
        }

        return null;
    }
}
