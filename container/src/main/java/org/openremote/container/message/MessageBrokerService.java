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

import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.snmp.SnmpComponent;
import org.apache.camel.impl.DefaultStreamCachingStrategy;
import org.apache.camel.spi.*;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.concurrent.CamelThreadFactory;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.concurrent.ContainerExecutor;
import org.openremote.container.concurrent.ContainerScheduledExecutor;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.DefaultWebsocketComponent;
import org.openremote.container.web.WebService;
import org.openremote.container.web.socket.WebsocketComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getString;

public class MessageBrokerService implements ContainerService {

    public static final String MESSAGE_SESSION_ALLOWED_ORIGIN = "MESSAGE_SESSION_ALLOWED_ORIGIN";
    public static final String MESSAGE_SESSION_ALLOWED_ORIGIN_DEFAULT = null;
    private static final Logger LOG = Logger.getLogger(MessageBrokerService.class.getName());
    public static final int PRIORITY = ContainerService.HIGH_PRIORITY;

    protected ProducerTemplate producerTemplate;
    protected MessageBrokerContext context;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void init(Container container) throws Exception {

        context = new MessageBrokerContext();

        final ExecutorServiceManager executorServiceManager = context.getExecutorServiceManager();
        executorServiceManager.setThreadNamePattern("#counter# #name#");
        executorServiceManager.setThreadPoolFactory(new ThreadPoolFactory() {
            @Override
            public ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
                // This is an unlimited pool used probably only be multicast aggregation
                return new ContainerExecutor(getExecutorName("MessagingPool", threadFactory), 1, Integer.MAX_VALUE, 10, -1);
            }

            @Override
            public ExecutorService newThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                // This pool is used by SEDA consumers, so the endpoint parameters define the pool and queue sizes
                return new ContainerExecutor(
                    getExecutorName("Messaging", threadFactory),
                    profile.getPoolSize(),
                    profile.getMaxPoolSize(),
                    profile.getKeepAliveTime(),
                    profile.getMaxQueueSize()
                );
            }

            @Override
            public ScheduledExecutorService newScheduledThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                return new ContainerScheduledExecutor(
                    getExecutorName("MessagingTasks", threadFactory),
                    profile.getPoolSize()
                );
            }

            protected String getExecutorName(String name, ThreadFactory threadFactory) {
                if (threadFactory instanceof CamelThreadFactory) {
                    CamelThreadFactory factory = (CamelThreadFactory) threadFactory;
                    String camelName = factory.getName();
                    camelName = camelName.contains("://") ? StringHelper.after(camelName, "://") : camelName;
                    camelName = camelName.contains("?") ? StringHelper.before(camelName, "?") : camelName;
                    name = name + "-" + camelName;
                }
                return name;
            }
        });

        // TODO make configurable in environment
        context.disableJMX();

        // TODO might need this for errorhandler?
        context.setAllowUseOriginalMessage(false);

        // Don't use JMS, we do our own correlation
        context.setUseBreadcrumb(false);

        // Force a quick shutdown of routes with in-flight exchanges
        context.getShutdownStrategy().setTimeout(1);
        context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

        context.setStreamCaching(true);
        StreamCachingStrategy streamCachingStrategy = new DefaultStreamCachingStrategy();
        streamCachingStrategy.setSpoolThreshold(524288); // Half megabyte
        context.setStreamCachingStrategy(streamCachingStrategy);

        context.setErrorHandlerBuilder(new org.apache.camel.builder.LoggingErrorHandlerBuilder() {
            @Override
            public Processor createErrorHandler(RouteContext routeContext, Processor processor) {
                // TODO: Custom error handler?
                return super.createErrorHandler(routeContext, processor);
            }
        });

        context.getRegistry().put(Container.class.getName(), container);

        String allowedOrigin = getString(container.getConfig(), MESSAGE_SESSION_ALLOWED_ORIGIN, MESSAGE_SESSION_ALLOWED_ORIGIN_DEFAULT);
        WebsocketComponent websocketComponent = new DefaultWebsocketComponent(
            container,
            allowedOrigin
        );

        context.addComponent(WebsocketComponent.NAME, websocketComponent);
        context.addComponent("snmp", new SnmpComponent());
    }

    @Override
    public void start(Container container) throws Exception {
        producerTemplate = context.createProducerTemplate();
        LOG.info("Starting Camel message broker");
        context.start();
    }

    @Override
    public void stop(Container container) throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    public MessageBrokerContext getContext() {
        return context;
    }

    public ProducerTemplate getProducerTemplate() {
        return producerTemplate;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
