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
package org.openremote.test;

import groovy.lang.Closure;
import org.keycloak.adapters.KeycloakDeployment;
import org.openremote.app.client.AppSecurity;
import org.openremote.app.client.OpenRemoteApp;
import org.openremote.app.client.rest.Requests;
import org.openremote.container.Container;
import org.openremote.model.http.ConstraintViolationReport;
import org.openremote.model.security.Tenant;

/**
 * Does the same job as or-app.
 */
public class TestOpenRemoteApp extends OpenRemoteApp {

    final TestAppSecurity security;

    final Tenant tenant;

    final Requests requests;

    public TestOpenRemoteApp(KeycloakDeployment keycloakDeployment, Tenant tenant, Closure<String> accessTokenClosure) {
        this.security = new TestAppSecurity(keycloakDeployment, accessTokenClosure);
        this.tenant = tenant;

        ClientObjectMapper<ConstraintViolationReport> constraintViolationReportMapper = new ClientObjectMapper<>(Container.JSON, ConstraintViolationReport.class);

        this.requests = new Requests(
            security::authorizeRequestParams,
            () -> {
            },
            () -> {
            },
            (e) -> {
                throw new RuntimeException(e.toString());
            },
            constraintViolationReportMapper::read
        );
    }

    @Override
    public AppSecurity getSecurity() {
        return security;
    }

    @Override
    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public Requests getRequests() {
        return requests;
    }
}
