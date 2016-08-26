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
package org.openremote.manager.client.i18n;

import com.google.gwt.i18n.client.LocalizableResource;
import com.google.gwt.i18n.client.Messages;

@LocalizableResource.DefaultLocale("en")
public interface ManagerMessages extends Messages {

    String logout();

    String sessionTimedOut();

    String map();

    String assets();

    String rules();

    String flows();

    String admin();

    String mapLoading();

    String loadingDotdotdot();

    String editAccount();

    String manageTenants();

    String manageUsers();

    String overview();

    String createTenant();

    String tenantName();

    String realm();

    String enabled();

    String newTenant();

    String editTenant();

    String tenantDisplayName();

    String updateTenant();

    String deleteTenant();

    String cancel();

    String filter();

    String search();

    String requiredFields();

    String newUser();

    String selectTenant();

    String username();

    String firstName();

    String lastName();

    String editUser();

    String updateUser();

    String createUser();

    String deleteUser();

    String email();

    String resetPassword();

    String repeatPassword();

    String notePasswordAfterCreate();

    String accessDenied();

    String requestFailed(String error);

    String noResponseFromServer();

    String badRequest();

    String conflictRequest();

    String errorMarshallingResponse(String error);

    String unexpectedResponseStatus(int statusCode, int expectedStatusCode);

    String unknownError();

    String tenantCreated(String displayName);

    String tenantUpdated(String displayName);

    String tenantDeleted(String displayName);

    String userDeleted(String username);

    String userCreated(String username);

    String userUpdated(String username);

    String passwordUpdated();

    String passwordsMustMatch();

    String roleLabel(@Select String roleName);

    String assignedRoles();

    String noteRolesAfterCreate();

    String showMoreAssets();

    String emptyAsset();

    String agentName();

    String connectorType();

    String configureAgents();

    String newAgent();

    String editAgent();

    String updateAgent();

    String createAgent();

    String deleteAgent();

    String description();

    String noConnectorAssigned();

    String connectorNotInstalled();

    String agentUpdated(String name);

    String agentCreated(String name);

    String agentDeleted(String name);

    String unsupportedAttributeType(String name);

    String loadingAssets();

    String assetName();

    String editAsset();

    String updateAsset();

    String createAsset();

    String deleteAsset();
}
