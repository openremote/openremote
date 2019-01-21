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

import org.openremote.container.Container;
import org.openremote.container.ContainerHealthStatusProvider;
import org.openremote.container.ContainerService;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.io.File;
import java.lang.management.ManagementFactory;

public class SystemHealthStatusProvider implements ContainerHealthStatusProvider {

    public static final String NAME = "system";
    public static final String VERSION = "1.0";

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

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
    public String getHealthStatusVersion() {
        return VERSION;
    }

    @Override
    public Value getHealthStatus() {
        ObjectValue objectValue = Values.createObject();
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        File[] roots = File.listRoots();

        objectValue.put("systemLoadPercentage", operatingSystemMXBean.getSystemCpuLoad()*100);
        objectValue.put("totalPhysicalMemoryMB", operatingSystemMXBean.getTotalPhysicalMemorySize() / (1024F*1024F));
        objectValue.put("freePhysicalMemoryMB", operatingSystemMXBean.getFreePhysicalMemorySize() / (1024F*1024F));
        objectValue.put("committedVirtualMemoryMB", operatingSystemMXBean.getCommittedVirtualMemorySize() / (1024F*1024F));
        objectValue.put("totalSwapSpaceMB", operatingSystemMXBean.getTotalSwapSpaceSize() / (1024F*1024F));
        objectValue.put("freeSwapSpaceMB", operatingSystemMXBean.getFreeSwapSpaceSize() / (1024F*1024F));

        ObjectValue rootsObj = Values.createObject();

        for (File root : roots) {
            ObjectValue rootObj = Values.createObject();
            rootObj.put("totalSpaceMB", root.getTotalSpace() / (1024F*1024F));
            rootObj.put("freeSpaceMB", root.getFreeSpace() / (1024F*1024F));
            String name = root.getAbsolutePath();
            name = name.replace("/","").replace("\\", "").replace(":", "");
            rootsObj.put(name, rootObj);
        }

        objectValue.put("filesystem", rootsObj);

        return objectValue;
    }
}
