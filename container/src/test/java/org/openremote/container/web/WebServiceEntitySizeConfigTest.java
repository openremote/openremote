package org.openremote.container.web;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.openremote.container.security.IdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebServiceEntitySizeConfigTest {

    @Test
    public void shouldUseDefaultEntitySizeLimits() throws Exception {
        TrackingWebService service = new TrackingWebService();
        service.init(new TestContainer(Map.of(
                WebService.OR_WEBSERVER_LISTEN_HOST, "127.0.0.1",
                WebService.OR_WEBSERVER_LISTEN_PORT, "0"
        )));

        assertEquals(WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE_DEFAULT, service.getMultipartLimit());
        assertEquals(WebService.OR_WEBSERVER_MAX_ENTITY_SIZE_DEFAULT, service.getMaxLimit());
    }

    @Test
    public void shouldUseConfiguredEntitySizeLimits() throws Exception {
        TrackingWebService service = new TrackingWebService();
        service.init(new TestContainer(Map.of(
                WebService.OR_WEBSERVER_LISTEN_HOST, "127.0.0.1",
                WebService.OR_WEBSERVER_LISTEN_PORT, "0",
                WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE, "1024",
                WebService.OR_WEBSERVER_MAX_ENTITY_SIZE, "2048"
        )));

        assertEquals(1024L, service.getMultipartLimit());
        assertEquals(2048L, service.getMaxLimit());
    }

    @Test
    public void shouldFallbackToDefaultsForInvalidEntitySizeLimits() throws Exception {
        TrackingWebService service = new TrackingWebService();
        service.init(new TestContainer(Map.of(
                WebService.OR_WEBSERVER_LISTEN_HOST, "127.0.0.1",
                WebService.OR_WEBSERVER_LISTEN_PORT, "0",
                WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE, "invalid",
                WebService.OR_WEBSERVER_MAX_ENTITY_SIZE, "also_invalid"
        )));

        assertEquals(WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE_DEFAULT, service.getMultipartLimit());
        assertEquals(WebService.OR_WEBSERVER_MAX_ENTITY_SIZE_DEFAULT, service.getMaxLimit());
    }

    static class TrackingWebService extends WebService {
        long getMultipartLimit() {
            return multipartMaxEntitySize;
        }

        long getMaxLimit() {
            return maxEntitySize;
        }
    }

    static class TestContainer implements Container {
        private final Map<String, String> config;

        TestContainer(Map<String, String> config) {
            this.config = config;
        }

        @Override
        public Map<String, String> getConfig() {
            return config;
        }

        @Override
        public boolean isDevMode() {
            return false;
        }

        @Override
        public ContainerService[] getServices() {
            return new ContainerService[0];
        }

        @Override
        public ScheduledExecutorService getScheduledExecutor() {
            return null;
        }

        @Override
        public ExecutorService getExecutor() {
            return null;
        }

        @Override
        public <T extends ContainerService> Collection<T> getServices(Class<T> type) {
            return Collections.emptyList();
        }

        @Override
        public <T extends ContainerService> T getService(Class<T> type) throws IllegalStateException {
            if (type == IdentityService.class) {
                return null;
            }
            throw new IllegalStateException("Service not available in test container: " + type.getName());
        }

        @Override
        public <T extends ContainerService> boolean hasService(Class<T> type) {
            return false;
        }

        @Override
        public MeterRegistry getMeterRegistry() {
            return null;
        }
    }
}
