/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.system;

import static java.lang.System.Logger.Level.WARNING;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

public class JvmHealthStatusProvider implements HealthStatusProvider {

  public static final String NAME = "jvm";
  public static final System.Logger LOG = System.getLogger(JvmHealthStatusProvider.class.getName());

  private List<AutoCloseable> closeables;

  @Override
  public void init(Container container) throws Exception {}

  @Override
  public void start(Container container) throws Exception {
    if (container.getMeterRegistry() != null) {
      this.closeables = new ArrayList<>();

      new JvmInfoMetrics().bindTo(container.getMeterRegistry());
      new JvmMemoryMetrics().bindTo(container.getMeterRegistry());
      JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
      closeables.add(jvmGcMetrics);
      jvmGcMetrics.bindTo(container.getMeterRegistry());
      new ProcessorMetrics().bindTo(container.getMeterRegistry());
      new JvmThreadMetrics().bindTo(container.getMeterRegistry());
      new JvmThreadDeadlockMetrics().bindTo(container.getMeterRegistry());
      JvmHeapPressureMetrics jvmHeapPressureMetrics = new JvmHeapPressureMetrics();
      closeables.add(jvmHeapPressureMetrics);
      jvmHeapPressureMetrics.bindTo(container.getMeterRegistry());
    }
  }

  @Override
  public void stop(Container container) throws Exception {
    if (closeables != null) {
      closeables.forEach(this::safeClose);
    }
  }

  private void safeClose(AutoCloseable it) {
    try {
      it.close();
    } catch (Exception e) {
      LOG.log(WARNING, "Error when closing {}", it, e);
    }
  }

  @Override
  public String getHealthStatusName() {
    return NAME;
  }

  @Override
  public Object getHealthStatus() {
    Map<String, Object> objectValue = new HashMap<>();
    com.sun.management.OperatingSystemMXBean operatingSystemMXBean =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    objectValue.put("startTimeMillis", runtimeMXBean.getStartTime());
    objectValue.put("upTimeMillis", runtimeMXBean.getUptime());
    objectValue.put("processLoadPercentage", operatingSystemMXBean.getProcessCpuLoad() * 100);
    objectValue.put(
        "heapMemoryUsageMB", memoryMXBean.getHeapMemoryUsage().getUsed() / (1024F * 1024F));
    objectValue.put(
        "nonHeapMemoryUsageMB", memoryMXBean.getNonHeapMemoryUsage().getUsed() / (1024F * 1024F));
    objectValue.put("peakThreadCount", threadMXBean.getPeakThreadCount());
    objectValue.put("threadCount", threadMXBean.getThreadCount());
    objectValue.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

    return objectValue;
  }
}
