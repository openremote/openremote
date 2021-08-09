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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.Database.PROPERTY_POOL_NAME;

public class PersistenceHealthStatusProvider implements HealthStatusProvider, ContainerService {

    private static final Logger LOG = Logger.getLogger(PersistenceHealthStatusProvider.class.getName());
    public static final String NAME = "db";
    public static final String VERSION = "1.0";
    protected PersistenceService persistenceService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

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
    public String getHealthStatusVersion() {
        return VERSION;
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

            ObjectNode value = ValueUtil.JSON.createObjectNode();
            value.put("idleConnections", idleConnections);
            value.put("activeConnections", activeConnections);
            value.put("totalConnections", totalConnections);
            value.put("threadsWaiting", threadsWaiting);

            return value;
        } catch (MalformedObjectNameException e) {
            LOG.log(Level.SEVERE, "Failed to get hikari connection pool status", e);
        }

        return null;
    }
}
