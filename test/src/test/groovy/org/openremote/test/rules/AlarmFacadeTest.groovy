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
package org.openremote.test.rules

import org.openremote.manager.alarm.AlarmService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.rules.RulesEngineId
import org.openremote.manager.rules.facade.AlarmFacade
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.model.alarm.Alarm
import org.openremote.model.alarm.SentAlarm
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.notification.Notification
import org.openremote.model.security.User
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE

/** Exercises creation against real asset paths, user roles, user links and alarm persistence. */
class AlarmFacadeTest extends Specification implements ManagerContainerTrait {

  private static final String SOURCE_ID = "4242"

  @Shared
  AlarmService alarmService

  @Shared
  AssetStorageService assetStorageService

  @Shared
  ManagerIdentityService identityService

  @Shared
  NotificationService originalNotificationService

  @Shared
  Map<String, ThingAsset> testAssets = [:]

  @Shared
  Map<String, User> testUsers = [:]

  @Shared
  String realm

  @Shared
  String otherRealm

  String alarmTitle
  List<UserAssetLink> createdUserLinks = []
  List<Notification> queuedNotifications = []

  def setupSpec() {
    def container = startContainer(defaultConfig(), defaultServices())
    alarmService = container.getService(AlarmService.class)
    assetStorageService = container.getService(AssetStorageService.class)
    originalNotificationService = container.getService(NotificationService.class)
    identityService = container.getService(ManagerIdentityService.class)
    def identityProvider = identityService.identityProvider
    def keycloakSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
    realm = keycloakSetup.realmBuilding.name
    otherRealm = keycloakSetup.realmCity.name

    testAssets.ancestor = createAsset("Ancestor", realm)
    testAssets.scoped = createAsset("Scoped", realm, testAssets.ancestor.id)
    testAssets.child = createAsset("Child", realm, testAssets.scoped.id)
    testAssets.grandchild = createAsset("Grandchild", realm, testAssets.child.id)
    testAssets.sibling = createAsset("Sibling", realm, testAssets.ancestor.id)
    testAssets.unrelated = createAsset("Unrelated", realm)
    testAssets.otherRealm = createAsset("Other realm", otherRealm)

    testUsers.unrestricted = identityProvider.getUser(keycloakSetup.testuser2Id)
    testUsers.restricted = identityProvider.getUser(keycloakSetup.testuser3Id)
    testUsers.otherRealm = identityProvider.getUser(keycloakSetup.testuser1Id)
    assert testUsers.unrestricted.realm == realm
    assert testUsers.restricted.realm == realm
    assert testUsers.otherRealm.realm != realm
    assert !identityProvider.getUserRealmRoles(realm, testUsers.unrestricted.id).contains(RESTRICTED_USER_REALM_ROLE)
    assert identityProvider.getUserRealmRoles(realm, testUsers.restricted.id).contains(RESTRICTED_USER_REALM_ROLE)
  }

  def setup() {
    alarmTitle = "Alarm facade ${UniqueIdentifierGenerator.generateId()}"
    // Capture the synchronous enqueue boundary, so absence checks do not race the notification
    // worker. Nothing from this specification is delivered to email or push providers.
    alarmService.notificationService = Stub(NotificationService) {
      sendNotificationAsync(_ as Notification, _ as Notification.Source, _ as String) >> { Notification notification, Notification.Source source, String sourceId ->
        queuedNotifications.add(notification)
      }
    }
  }

  def cleanup() {
    alarmService.notificationService = originalNotificationService
    // Look up by this iteration's unique title, including alarms persisted by a failing rejection
    // test. Do not depend on the facade returning the ID before an assertion or exception fails.
    createdAlarms().each { alarmService.removeAlarm(it) }
    if (!createdUserLinks.isEmpty()) {
      assetStorageService.deleteUserAssetLinks(createdUserLinks)
    }
  }

  def cleanupSpec() {
    if (assetStorageService != null && !testAssets.isEmpty()) {
      assert assetStorageService.delete(testAssets.values().collect { it.id })
    }
  }

  def "#scope scope creating an alarm for #targetName is allowed: #allowed"() {
    given:
    def facade = facadeFor(scope)
    def targetId = testAssets[targetName].id
    def alarm = newAlarm()

    when:
    def alarmId = facade.create(alarm, [targetId])

    then:
    if (allowed) {
      assertCreated(alarmId, scope, null, [targetId])
    } else {
      verifyAll {
        alarmId == null
        createdAlarms().isEmpty()
        queuedNotifications.isEmpty()
      }
    }

    where:
    scope   | targetName   | allowed
    "realm" | "ancestor"   | true
    "realm" | "sibling"    | true
    "realm" | "unrelated"  | true
    "asset" | "scoped"     | true
    "asset" | "child"      | true
    "asset" | "grandchild" | true
    "asset" | "ancestor"   | false
    "asset" | "sibling"    | false
    "asset" | "unrelated"  | false
  }

  def "#scope scope rejects a foreign-realm asset before creating the alarm"() {
    when:
    facadeFor(scope).create(newAlarm(), [testAssets.otherRealm.id])

    then:
    thrown(AlarmService.AlarmAssetLinkRealmMismatchException)
    createdAlarms().isEmpty()
    queuedNotifications.isEmpty()

    where:
    scope << ["realm", "asset"]
  }

  def "asset scope rejects the complete mixed target list #targetNames before notifying"() {
    given:
    def alarm = newAlarm(testUsers.unrestricted.id, Alarm.Severity.HIGH)
    def targetIds = targetNames.collect { testAssets[it].id }

    when:
    def alarmId = facadeFor("asset").create(alarm, targetIds)

    then:
    verifyAll {
      alarmId == null
      createdAlarms().isEmpty()
      queuedNotifications.isEmpty()
    }

    where:
    targetNames << [["child", "sibling"], ["sibling", "child"]]
  }

  def "#scope scope accepts duplicate allowed asset IDs and keeps one link per asset"() {
    given:
    def targetId = testAssets.child.id

    when:
    def alarmId = facadeFor(scope).create(newAlarm(), [targetId, targetId])

    then:
    assertCreated(alarmId, scope, null, [targetId])
    queuedNotifications.isEmpty()

    where:
    scope << ["realm", "asset"]
  }

  def "#scope scope overrides the supplied alarm realm with the engine realm"() {
    given:
    def alarm = newAlarm().setRealm(otherRealm)

    when:
    def alarmId = facadeFor(scope).create(alarm, [testAssets.child.id])

    then:
    assertCreated(alarmId, scope, null, [testAssets.child.id])

    where:
    scope << ["realm", "asset"]
  }

  def "#scope scope assigning #assignee to #targetNames with links #linkedNames is allowed: #allowed"() {
    given:
    def user = testUsers[assignee]
    def targetIds = targetNames.collect { testAssets[it].id }
    linkedNames.each { name ->
      createdUserLinks.add(new UserAssetLink(realm, user.id, testAssets[name].id))
    }
    assetStorageService.storeUserAssetLinks(createdUserLinks)
    // These are newly created assets: verify precisely which targets the assignee can access.
    targetNames.each { name ->
      assert assetStorageService.isUserAsset(user.id, testAssets[name].id) == linkedNames.contains(name)
    }
    def alarm = newAlarm(user.id, Alarm.Severity.HIGH)

    when:
    def alarmId = facadeFor(scope).create(alarm, targetIds)

    then:
    if (allowed) {
      assertCreated(alarmId, scope, user.id, targetIds)
      // Positive control: the capture is connected to the real alarm service's notification path.
      assert queuedNotifications.size() == 2
      assert queuedNotifications.any { notification ->
        notification.targets?.any { it.type == Notification.TargetType.USER && it.id == user.id }
      }
    } else {
      verifyAll {
        alarmId == null
        createdAlarms().isEmpty()
        queuedNotifications.isEmpty()
      }
    }

    where:
    [scope, assignee, targetNames, linkedNames, allowed] << ["realm", "asset"].collectMany { scope ->
      assigneeCases().collect { row -> [scope] + row }
    }
  }

  def "#scope scope rejects a missing assignee before creating or notifying"() {
    when:
    def alarmId = facadeFor(scope).create(
            newAlarm(UniqueIdentifierGenerator.generateId(), Alarm.Severity.HIGH), [testAssets.child.id])

    then:
    verifyAll {
      alarmId == null
      createdAlarms().isEmpty()
      queuedNotifications.isEmpty()
    }

    where:
    scope << ["realm", "asset"]
  }

  private static List<List> assigneeCases() {
    [
      // Unrestricted users need no links, even when they have links elsewhere.
      ["unrestricted", ["child"], [], true],
      ["unrestricted", ["child"], ["unrelated"], true],
      ["unrestricted", ["child", "grandchild"], [], true],
      // Restricted users require explicit links to every target, not just a scope/ancestor link.
      ["restricted", ["scoped"], ["scoped"], true],
      ["restricted", ["child"], ["child"], true],
      ["restricted", ["child", "child"], ["child"], true],
      ["restricted", ["child", "grandchild"], ["child", "grandchild"], true],
      ["restricted", ["child"], [], false],
      ["restricted", ["child"], ["scoped"], false],
      ["restricted", ["child"], ["sibling"], false],
      ["restricted", ["child", "grandchild"], ["child"], false],
      ["otherRealm", ["child"], [], false]
    ]
  }

  private ThingAsset createAsset(String name, String assetRealm, String parentId = null) {
    assetStorageService.merge(new ThingAsset("Alarm facade ${name}").setRealm(assetRealm).setParentId(parentId))
  }

  private AlarmFacade facadeFor(String scope) {
    def engineId = scope == "asset" ? new RulesEngineId(realm, testAssets.scoped.id) : new RulesEngineId(realm)
    new AlarmFacade(engineId, alarmService, assetStorageService, identityService)
  }

  private Alarm newAlarm(String assigneeId = null, Alarm.Severity severity = Alarm.Severity.MEDIUM) {
    new Alarm(alarmTitle, "Alarm facade scope regression", severity, assigneeId, realm)
            .setSourceId(SOURCE_ID)
  }

  private List<SentAlarm> createdAlarms() {
    alarmService.getAlarms(realm, null, null, null).findAll { it.title == alarmTitle }
  }

  private void assertCreated(Long alarmId, String scope, String assigneeId, List<String> targetIds) {
    assert alarmId != null
    def alarms = createdAlarms()
    assert alarms.size() == 1
    def alarm = alarms.first()
    assert alarm.id == alarmId
    assert alarm.realm == realm
    assert alarm.source == (scope == "asset" ? Alarm.Source.ASSET_RULESET : Alarm.Source.REALM_RULESET)
    assert alarm.sourceId == SOURCE_ID
    assert alarm.assigneeId == assigneeId
    def links = alarmService.getAssetLinks(alarmId, realm)
    assert links.size() == targetIds.toSet().size()
    assert links.collect { it.id.assetId }.toSet() == targetIds.toSet()
  }
}
