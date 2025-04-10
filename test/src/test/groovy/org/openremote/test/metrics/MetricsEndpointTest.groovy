package org.openremote.test.metrics


import org.openremote.manager.system.HealthService
import org.openremote.model.Container
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.Constants.MASTER_REALM

class MetricsEndpointTest extends Specification implements ManagerContainerTrait {

    def "Test metrics endpoint existence and content"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig() << [(Container.OR_METRICS_ENABLED): "true"], defaultServices())

        and: "a resource client is created"
        // Resteasy client has issues with @Suspended annotation so not used for now
        //def datapointResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetDatapointResource.class)
        def requestTarget = getClientTarget(serverUri(HealthService.OR_METRICS_PORT_DEFAULT).path("metrics"), MASTER_REALM)

        when: "requesting the metrics endpoint"
        def response = requestTarget.request().get()

        then: "the response should contain metrics data"
        response.getStatus() == 200
        def responseStr = response.readEntity(String.class)
        responseStr.contains("executor_pool_core_threads{name=\"ContainerExecutor\"}")

        cleanup: "clean up"
        if (response != null) {
            response.close()
        }
    }

}
