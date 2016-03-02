package org.openremote.manager.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.openremote.manager.server.contextbroker.ContextBrokerService;
import org.openremote.manager.server.identity.IdentityService;
import org.openremote.manager.server.map.MapService;
import org.openremote.manager.server.persistence.PersistenceService;
import org.openremote.manager.server.web.WebService;

import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.DEV_MODE;
import static org.openremote.manager.server.Constants.DEV_MODE_DEFAULT;
import static org.openremote.manager.server.SampleData.IMPORT_SAMPLE_DATA;
import static org.openremote.manager.server.SampleData.IMPORT_SAMPLE_DATA_DEFAULT;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOG = Logger.getLogger(ServerVerticle.class.getName());

    protected boolean devMode;
    protected SampleData sampleData;

    protected IdentityService identityService;
    protected ContextBrokerService contextBrokerService;
    protected MapService mapService;
    protected PersistenceService persistenceService;
    protected WebService webService;

    @Override
    public void start(Future<Void> startFuture) {

        devMode = config().getBoolean(DEV_MODE, DEV_MODE_DEFAULT);
        boolean importSampleData = config().getBoolean(IMPORT_SAMPLE_DATA, IMPORT_SAMPLE_DATA_DEFAULT);

        vertx.executeBlocking(
            blocking -> {

                identityService = new IdentityService();
                identityService.start(vertx, config());

                contextBrokerService = new ContextBrokerService();
                contextBrokerService.start(vertx, config());

                mapService = new MapService();
                mapService.start(vertx, config());

                persistenceService = new PersistenceService();
                persistenceService.start(config());

                if (devMode || importSampleData) {
                    sampleData = new SampleData();
                    sampleData.create(
                        identityService,
                        contextBrokerService,
                        persistenceService
                    );
                }

                webService = new WebService(
                    identityService,
                    contextBrokerService,
                    mapService,
                    persistenceService
                );

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
            if (webService != null)
                webService.stop();
            if (persistenceService != null)
                persistenceService.stop();
            if (mapService != null)
                mapService.stop();
            if (contextBrokerService != null)
                contextBrokerService.stop();
            if (identityService != null)
                identityService.stop();
            stopFuture.complete();
        } catch (Exception ex) {
            stopFuture.fail(ex);
        }
    }
}