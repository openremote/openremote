/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.setup.demo;

import org.openremote.manager.dashboard.DashboardStorageService;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.util.ValueUtil;

import java.io.InputStream;

public class ManagerDemoDashboardSetup extends ManagerSetup {

    protected final DashboardStorageService dashboardStorageService;

    public ManagerDemoDashboardSetup(Container container) {
        super(container);
        this.dashboardStorageService = container.getService(DashboardStorageService.class);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // SmartCity
        try (InputStream inputStream = ManagerDemoDashboardSetup.class.getResourceAsStream("/demo/dashboards/smartcity/parking.json")) {
            Dashboard dashboard = ValueUtil.JSON.readValue(inputStream, Dashboard.class);
            dashboardStorageService.createNew(dashboard);
        }

        // Manufacturer
        try (InputStream inputStream = ManagerDemoDashboardSetup.class.getResourceAsStream("/demo/dashboards/manufacturer/harvesting.json")) {
            Dashboard dashboard = ValueUtil.JSON.readValue(inputStream, Dashboard.class);
            dashboardStorageService.createNew(dashboard);
        }

    }
}
