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

import io.micrometer.core.instrument.Tags;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Route;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.spi.RoutePolicy;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.util.MapAccess;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.system.StatusResource;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.apache.camel.component.micrometer.MicrometerConstants.ROUTE_ID_TAG;

/**
 * This service is here to initialise the discovered {@link HealthStatusProvider}s and ({@link StatusResource}
 */
public class HealthService implements ContainerService {

    /**
     * Customised {@link MicrometerRoutePolicy} to add SEDA queue metrics
     */
    protected static class SedaMicrometerRoutePolicy extends MicrometerRoutePolicy {
        @Override
        public void onInit(Route route) {
            if (route.getEndpoint() instanceof SedaEndpoint sedaEndpoint) {
                getMeterRegistry().gauge("or.camel.route.queue.size", getNamingStrategy().getTags(route), sedaEndpoint, SedaEndpoint::getCurrentQueueSize);
            }
            super.onInit(route);
        }
    }

    public static final System.Logger LOG = System.getLogger(HealthService.class.getName());
    public static final String OR_METRICS_PORT = "OR_METRICS_PORT";
    public static final int OR_METRICS_PORT_DEFAULT = 8404;
    public static final String OR_CAMEL_ROUTE_METRIC_PREFIX = "or_camel_route";
    protected List<HealthStatusProvider> healthStatusProviderList = new ArrayList<>();
    protected boolean metricsEnabled;
    protected HTTPServer metricsServer;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        metricsEnabled = container.getMeterRegistry() != null;
        ServiceLoader.load(HealthStatusProvider.class).forEach(hsp -> {
            LOG.log(System.Logger.Level.INFO, "Health Status Provider: " + hsp.getClass());
            healthStatusProviderList.add(hsp);
        });

        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            healthStatusProvider.init(container);
        }

        container.getService(ManagerWebService.class).addApiSingleton(
            new StatusResourceImpl(container, healthStatusProviderList)
        );


        if (metricsEnabled) {
            MessageBrokerService brokerService = container.getService(MessageBrokerService.class);
            int metricsPort = MapAccess.getInteger(container.getConfig(), OR_METRICS_PORT, OR_METRICS_PORT_DEFAULT);
            LOG.log(System.Logger.Level.INFO, "Metrics collection enabled");

            // Add additional web server for metrics (this keeps CI/CD prometheus scraper config simple with no oauth
            // this port can be exposed to the host but not to the public
            metricsServer = new HTTPServer.Builder()
                .withPort(metricsPort)
                .withExecutorService(container.getExecutorService())
                .build();

            // Alternative servlet option to run on existing undertow but would require oauth on prometheus scraper
//            DeploymentInfo prometheusServlet = Servlets.deployment()
//                .setClassLoader(Container.class.getClassLoader())
//                .setContextPath(METRICS_PATH)
//                .setDeploymentName("Metrics")
//                .addServlets(
//                    Servlets.servlet("MetricsServlet", MetricsServlet.class)
//                        .addMapping("/*")
//                    .setServletSecurityInfo(
//                        new ServletSecurityInfo().addRoleAllowed(Constants.READ_ADMIN_ROLE)
//                    )
//                );
//            HttpHandler handler = WebService.addServletDeployment(container, prometheusServlet, true);
//            container.getService(ManagerWebService.class).getRequestHandlers().add(0, new WebService.RequestHandler("Prometheus Metrics", exchange -> exchange.getRequestPath().equals(METRICS_PATH), handler));

            // Configure camel metrics collection here as need access to the custom registry
            MicrometerRoutePolicyFactory micrometerRoutePolicyFactory = new MicrometerRoutePolicyFactory() {
                @Override
                public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode routeDefinition) {
                    SedaMicrometerRoutePolicy answer = new SedaMicrometerRoutePolicy();
                    answer.setMeterRegistry(getMeterRegistry());
                    answer.setPrettyPrint(isPrettyPrint());
                    answer.setDurationUnit(getDurationUnit());
                    answer.setNamingStrategy(getNamingStrategy());
                    return answer;
                }
            };
            micrometerRoutePolicyFactory.setNamingStrategy(new MicrometerRoutePolicyNamingStrategy() {
                @Override
                public String getName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX;
                }

                @Override
                public String getExchangesSucceededName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX + "_succeeded";
                }

                @Override
                public String getExchangesFailedName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX + "_failed";
                }

                @Override
                public String getExchangesTotalName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX + "_total";
                }

                @Override
                public String getFailuresHandledName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX + "_failed_handled";
                }

                @Override
                public String getExternalRedeliveriesName(Route route) {
                    return OR_CAMEL_ROUTE_METRIC_PREFIX + "_redeliveries";
                }

                @Override
                public Tags getTags(Route route) {
                    return Tags.of(
                        ROUTE_ID_TAG, route.getId());
                }

                @Override
                public Tags getExchangeStatusTags(Route route) {
                    return Tags.of(
                        ROUTE_ID_TAG, route.getId());
                }
            });
            micrometerRoutePolicyFactory.setMeterRegistry(container.getMeterRegistry());
            brokerService.getContext().addRoutePolicyFactory(micrometerRoutePolicyFactory);
        } else {
            LOG.log(System.Logger.Level.INFO, "Metrics collection disabled");
        }
    }

    @Override
    public void start(Container container) throws Exception {
        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            healthStatusProvider.start(container);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (metricsServer != null) {
            metricsServer.close();
        }
        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            healthStatusProvider.stop(container);
        }
    }
}
