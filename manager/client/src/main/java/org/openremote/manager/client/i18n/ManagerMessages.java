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

@LocalizableResource.DefaultLocale
public interface ManagerMessages extends Messages {

    String logout();

    String sessionTimedOut();

    String map();

    String assets();

    String rules();

    String apps();

    String admin();

    String lastModifiedOn();

    String loadingDotdotdot();

    String editAccount();

    String manageTenants();

    String manageUsers();

    String createTenant();

    String tenantName();

    String realm();

    String enabled();

    String editTenant();

    String tenantDisplayName();

    String updateTenant();

    String deleteTenant();

    String OK();

    String cancel();

    String filter();

    String search();

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

    String unexpectedResponseStatus(int statusCode, String expectedStatusCodes);

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

    String description();

    String unsupportedAttributeType(String name);

    String unsupportedMetaItemType(String name);

    String unsupportedValueType(String name);

    String loadingAssets();

    String assetName();

    String updateAsset();

    String createAsset();

    String deleteAsset();

    String assetType();

    String createdOn();

    String selectedLocation();

    String assetCreated(String name);

    String assetUpdated(String name);

    String assetDeleted(String name);

    String location();

    String selectLocation();

    String confirmation();

    String confirmationDelete(String label);

    String parentAsset();

    String selectAsset();

    String selectAssetDescription();

    String invalidAssetParent();

    String centerMap();

    String showHistory();

    String sort();

    String assetTypeLabel(@Select String name);

    String enterCustomAssetType();

    String read();

    String write();

    String noAgentsFound();

    String deleteAttribute();

    String addAttribute();

    String itemName();

    String deleteItem();

    String newMetaItems();

    String addItem();

    String type();

    String value();

    String metaItems();

    String protocolLinks();

    String protocolLinkDiscovery();

    String protocolLinkDiscoveryParent();

    String uploadProtocolFile();

    String importInProgress();

    String or();

    String and();

    String customItem();

    String enterCustomAssetAttributeMetaName();

    String selectType();

    String valueTypeDisplayName(@Select String valueType);

    String metaItemDisplayName(@Select String metaItemName);

    String attributeDeleted(String name);

    String attributeAdded(String name);

    String newAttribute();

    String attributes();

    String attributeName();

    String fullscreen();

    String selectConsoleApp();

    String manageGlobalRulesets();

    String manageTenantRulesets();

    String newRuleset();

    String rulesetName();

    String editGlobalRules();

    String manageTenantAssets();

    String manageAssetRulesets();

    String uploadRulesFile();

    String updateRuleset();

    String createRuleset();

    String deleteRuleset();

    String rulesetCreated(String name);

    String rulesetDeleted(String name);

    String rulesetUpdated(String name);

    String downloadRulesFile();

    String editTenantRuleset();

    String editAssetRuleset();

    String attributeWriteSent(String name);

    String duplicateAttributeName();

    String invalidAttributeName();

    String invalidAttributeType();

    String editAsset();

    String viewAsset();

    String errorLoadingTenant(int statusCode);

    String subscriptionFailed(String eventType);

    String datapointInterval(@Select String name);

    String showChartAggregatedFor();

    String historicalData();

    String canvasNotSupported();

    String previous();

    String next();

    String noRulesetsFound();

    String noTenantsFound();

    String noUserFound();

    String showLiveUpdates();

    String refreshAllAttributes();

    String start();

    String repeat();

    String getStatus();

    String commandRequestSent(String name);

    String attributeType(@Select String name);

    String validationFailureOnAttribute(String attributeName);

    String validationFailureOnMetaItem(String attributeName, String metaItemName);

    String validationFailure(@Optional String parameter, @Select String name);

    String validationFailureParameter(@Select String parameter);

    String syslog();

    String pauseLog();

    String continueLog();

    String clear();

    String noLogMessagesReceived();

    String jsonObject();

    String jsonArray();

    String edit();

    String emptyJsonData();

    String reset();

    String executable();

    String template();

    String templateAsset();

    String invalidTemplateAsset();

    String noAssetSelected();

    String clearSelection();

    String protocolConfiguration();

    String showLast();

    String events();

    String store();

    String eventsFor();

    String saveSettings();

    String removeAll();

    String minutes();

    String hours();

    String days();

    String eventsRemoved();

    String settingsSaved();

    String noAttributes();

    String simulator();

    String invalidValues();

    String writeSimulatorState();

    String simulatorStateSubmitted();

    String selectAgent();

    String selectAttribute();

    String selectProtocolConfiguration();

    String protocolLinkDiscoveryStarted();

    String protocolLinkDiscoverySuccess(int assetCount);

    String protocolLinkDiscoveryFailure(int failureCode);

    String attributeLinkConverterValues();

    String attributeLinkNewConverterValue();

    String enterConverterValue();

    String selectConverter();

    String customConverter();

    String converterType(@Select String type);

    String addConverterValue();

    String deleteConverterValue();

    String linkAssetUsers();

    String waitingForStatus();

    String noAttributesLinkedToSimulator();
}
