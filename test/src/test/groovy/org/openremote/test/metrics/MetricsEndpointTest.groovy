/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.test.metrics


import org.openremote.manager.system.HealthService
import org.openremote.model.util.Config
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.Constants.MASTER_REALM

class MetricsEndpointTest extends Specification implements ManagerContainerTrait {

    def "Test metrics endpoint existence and content"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig() << [(Config.OR_METRICS_ENABLED): "true"], defaultServices())

        and: "a resource client is created"
        // Resteasy client has issues with @Suspended annotation so not used for now
        //def datapointResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetDatapointResource.class)
        def requestTarget = getClientTarget(serverUri(HealthService.OR_METRICS_PORT_DEFAULT).path("metrics"), null)

        when: "requesting the metrics endpoint"
        def response = requestTarget.request().get()

        then: "the response should contain metrics data"
        response.withCloseable { r ->
            def responseStr = r.readEntity(String.class)
            assert responseStr.contains("executor_pool_core_threads{name=\"ContainerExecutor\"}")
            assert responseStr.contains("jvm_info")
            assert responseStr.contains("jvm_buffer_")
            assert responseStr.contains("jvm_gc_")
            assert responseStr.contains("jvm_memory_")
            assert responseStr.contains("jvm_threads_")
            assert r.status == 200
            return true
        }

        cleanup: "clean up"
        if (response != null) {
            response.close()
        }
    }

}
