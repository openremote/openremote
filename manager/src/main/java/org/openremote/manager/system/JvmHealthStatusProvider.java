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
package org.openremote.manager.system;

import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

public class JvmHealthStatusProvider implements HealthStatusProvider {

    public static final String NAME = "jvm";

    @Override
    public void init(Container container) throws Exception {

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
        Map<String, Object> objectValue = new HashMap<>();
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        objectValue.put("startTimeMillis", runtimeMXBean.getStartTime());
        objectValue.put("upTimeMillis", runtimeMXBean.getUptime());
        objectValue.put("processLoadPercentage", operatingSystemMXBean.getProcessCpuLoad()*100);
        objectValue.put("heapMemoryUsageMB", memoryMXBean.getHeapMemoryUsage().getUsed() / (1024F*1024F));
        objectValue.put("nonHeapMemoryUsageMB", memoryMXBean.getNonHeapMemoryUsage().getUsed() / (1024F*1024F));
        objectValue.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        objectValue.put("threadCount", threadMXBean.getThreadCount());
        objectValue.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

        return objectValue;
    }
}
