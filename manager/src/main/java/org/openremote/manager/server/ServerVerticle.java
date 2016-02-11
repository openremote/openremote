package org.openremote.manager.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.openremote.manager.server.service.ContextBrokerService;
import org.openremote.manager.server.service.PersistenceService;
import org.openremote.manager.server.service.WebService;

import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.DEV_MODE;
import static org.openremote.manager.server.Constants.DEV_MODE_DEFAULT;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOG = Logger.getLogger(ServerVerticle.class.getName());

    protected boolean devMode;
    protected SampleData sampleData;

    protected ContextBrokerService contextBrokerService;
    protected PersistenceService persistenceService;
    protected WebService webService;

    @Override
    public void start(Future<Void> startFuture) {

        devMode = config().getBoolean(DEV_MODE, DEV_MODE_DEFAULT);

        vertx.executeBlocking(
            blocking -> {

                contextBrokerService = new ContextBrokerService();
                contextBrokerService.start(vertx, config());

                persistenceService = new PersistenceService();
                persistenceService.start(config());

                if (devMode) {
                    sampleData = new SampleData();
                    sampleData.create(contextBrokerService, persistenceService);
                }

                webService = new WebService(contextBrokerService, persistenceService);
                webService.start(vertx, config(), event -> {
                    if (event.succeeded()) {
                        blocking.complete();
                    } else {
                        blocking.fail(event.cause());
                    }
                });
            },
            result -> {
                if (result.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(result.cause());
                }
            }
        );
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        try {
            if (devMode && sampleData != null)
                sampleData.drop(contextBrokerService, persistenceService);
            if (webService != null)
                webService.stop();
            if (persistenceService != null)
                persistenceService.stop();
            if (contextBrokerService != null)
                contextBrokerService.stop();
            stopFuture.complete();
        } catch (Exception ex) {
            stopFuture.fail(ex);
        }
    }
}