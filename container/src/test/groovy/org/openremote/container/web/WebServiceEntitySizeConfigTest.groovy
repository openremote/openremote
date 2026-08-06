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
package org.openremote.container.web

import org.openremote.container.security.IdentityService
import org.openremote.model.Container
import spock.lang.Specification
import spock.lang.Unroll

class WebServiceEntitySizeConfigTest extends Specification {

    @Unroll
    def "uses #scenario entity size limits"() {
        given:
        def config = [
            (WebService.OR_WEBSERVER_LISTEN_HOST): "127.0.0.1",
            (WebService.OR_WEBSERVER_LISTEN_PORT): "0"
        ] + overrides
        def container = Stub(Container) {
            getConfig() >> config
            getService(IdentityService) >> null
            getService(_) >> { Class type ->
                throw new IllegalStateException("Service not available in test container: " + type.name)
            }
        }
        def service = new TrackingWebService()

        when:
        service.init(container)

        then:
        service.multipartLimit == expectedMultipartLimit
        service.maxLimit == expectedMaxLimit

        where:
        scenario     | overrides                                                                                                                 || expectedMultipartLimit                                    | expectedMaxLimit
        "default"    | [:]                                                                                                                       || WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE_DEFAULT | WebService.OR_WEBSERVER_MAX_ENTITY_SIZE_DEFAULT
        "configured" | [(WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE): "1024", (WebService.OR_WEBSERVER_MAX_ENTITY_SIZE): "2048"]           || 1024L                                                     | 2048L
        "invalid"    | [(WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE): "invalid", (WebService.OR_WEBSERVER_MAX_ENTITY_SIZE): "also_invalid"] || WebService.OR_WEBSERVER_MULTIPART_MAX_ENTITY_SIZE_DEFAULT | WebService.OR_WEBSERVER_MAX_ENTITY_SIZE_DEFAULT
    }

    static class TrackingWebService extends WebService {
        long getMultipartLimit() {
            multipartMaxEntitySize
        }

        long getMaxLimit() {
            maxEntitySize
        }
    }
}
