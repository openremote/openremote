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
package org.openremote.container.message;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.component.snmp.SnmpComponent;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultStreamCachingStrategy;
import org.apache.camel.impl.health.ConsumersHealthCheckRepository;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.DefaultHealthCheckRegistry;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.ThreadPoolFactory;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.DefaultThreadPoolFactory;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.concurrent.CamelThreadFactory;
import org.openremote.container.concurrent.ContainerScheduledExecutor;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class MessageBrokerService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MessageBrokerService.class.getName());
    public static final int PRIORITY = ContainerService.LOW_PRIORITY;

    protected DefaultCamelContext context = new DefaultCamelContext();
    protected ProducerTemplate producerTemplate = context.createProducerTemplate();
    protected FluentProducerTemplate fluentProducerTemplate = context.createFluentProducerTemplate();

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void init(Container container) throws Exception {

        MeterRegistry meterRegistry = container.getMeterRegistry();
        final ExecutorServiceManager executorServiceManager = context.getExecutorServiceManager();

        // Not using InstrumentedThreadPoolFactory directly as it only uses the ThreadPoolProfile ID for naming
        ThreadPoolFactory threadPoolFactory = new DefaultThreadPoolFactory() {
            private static final AtomicLong COUNTER = new AtomicLong();

            @Override
            public ExecutorService newThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {

                ExecutorService executorService;

                // SEDA routes always create a custom thread pool so we will force them to use our shared executor to
                // minimise the number of threads
                executorService = container.getExecutor();

//                // Force any endpoints that use the default profile to use a single built in executor to avoid excessive thread creation
//                if (profile.isDefaultProfile()) {
//                    executorService = container.getExecutor();
//                } else {
//                    executorService = super.newThreadPool(profile, threadFactory);
//
//                    // Want to instrument pools that use defaultThreadPool profile (ProducerTemplate and multiple consumer SEDA endpoints)
//                    if (meterRegistry != null) {
//                        String name = getExecutorName("Pool", threadFactory);
//                        name = "Pool".equals(name) ? profile.getId() : name;
//                        executorService = ExecutorServiceMetrics.monitor(meterRegistry, executorService, name(name));
//                    }
//                }

                return executorService;
            }

            @Override
            public ScheduledExecutorService newScheduledThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                ScheduledExecutorService scheduledExecutorService;

                // Force any endpoints that use the default profile to use a single built in executor to avoid excessive thread creation
                if (profile.isDefaultProfile()) {
                    scheduledExecutorService = container.getScheduledExecutor();
                } else {
                    scheduledExecutorService = super.newScheduledThreadPool(profile, threadFactory);

                    // Disabled as not very useful for SEDA components
//                    if (meterRegistry != null) {
//                        String name = getExecutorName("", threadFactory);
//                        name = "ScheduledPool".equals(name) ? profile.getId() : name;
//                        scheduledExecutorService = new TimedScheduledExecutorService(meterRegistry, scheduledExecutorService, name(name), Tags.empty());
//                    }
                }

                return scheduledExecutorService;
            }

            @Override
            public ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
                return super.newCachedThreadPool(threadFactory);
            }

            protected String getExecutorName(String name, ThreadFactory threadFactory) {
                if (threadFactory instanceof CamelThreadFactory factory) {
                    String camelName = factory.getName();
                    camelName = camelName.contains("://") ? StringHelper.after(camelName, "://") : camelName;
                    camelName = camelName.contains("?") ? StringHelper.before(camelName, "?") : camelName;
                    name = name + "-" + camelName;
                }
                return name;
            }

            private String name(String prefix) {
                return prefix + COUNTER.incrementAndGet();
            }
        };

        executorServiceManager.setThreadNamePattern("#counter# #name#");
        executorServiceManager.setThreadPoolFactory(threadPoolFactory);

        // TODO might need this for errorhandler?
        context.setAllowUseOriginalMessage(false);

        // Don't use JMS, we do our own correlation
        context.setUseBreadcrumb(false);

        // Enable health checks - Have to manually add built in ones for some reason
        context.setLoadHealthChecks(true);
        final var checkRegistry = new DefaultHealthCheckRegistry();
        checkRegistry.setExposureLevel("full");
        checkRegistry.register(new RoutesHealthCheckRepository());
        checkRegistry.register(new ConsumersHealthCheckRepository());
        checkRegistry.register(new ContextHealthCheck());
        context.getCamelContextExtension().addContextPlugin(HealthCheckRegistry.class, checkRegistry);

        // Force a quick shutdown of routes with in-flight exchanges
        context.getShutdownStrategy().setTimeout(5);
        context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

        context.setStreamCaching(true);
        StreamCachingStrategy streamCachingStrategy = new DefaultStreamCachingStrategy();
        streamCachingStrategy.setSpoolThreshold(524288); // Half megabyte
        context.setStreamCachingStrategy(streamCachingStrategy);

        context.getCamelContextExtension().setErrorHandlerFactory(new DefaultErrorHandlerBuilder());

        if (container.isDevMode()) {
            context.setMessageHistory(true);
            context.setSourceLocationEnabled(true);
        }

        ((SimpleRegistry)((DefaultRegistry)context.getRegistry()).getFallbackRegistry()).put("OpenRemote", Map.of(Container.class, container));

        context.addComponent("snmp", new SnmpComponent());
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("Starting Camel message broker");
        context.start();
    }

    @Override
    public void stop(Container container) throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    public DefaultCamelContext getContext() {
        return context;
    }

    public ProducerTemplate getProducerTemplate() {
        return producerTemplate;
    }

    public FluentProducerTemplate getFluentProducerTemplate() {
        return fluentProducerTemplate;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
