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

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.rules.RulesEngineId
import org.openremote.manager.rules.facade.HistoricFacade
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HistoricFacadeTest extends Specification implements ManagerContainerTrait {

  private static final String ATTRIBUTE_NAME = "history"

  AssetStorageService assetStorageService
  AssetDatapointService historicService
  Map<String, ThingAsset> testAssets = [:]
  List<String> createdAssetIds = []
  String realm
  LocalDateTime timestamp

  def setup() {
    def container = startContainer(defaultConfig(), defaultServices())
    stopPseudoClock()
    assetStorageService = container.getService(AssetStorageService.class)
    historicService = container.getService(AssetDatapointService.class)
    def keycloakSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
    realm = keycloakSetup.realmBuilding.name
    timestamp = getInstantTimeOf(container).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .minusHours(1).truncatedTo(ChronoUnit.SECONDS)

    // Siblings share an ancestor with the scope asset but are outside its subtree.
    testAssets.ancestor = createAsset("Ancestor", realm)
    testAssets.scoped = createAsset("Scoped", realm, testAssets.ancestor.id)
    testAssets.child = createAsset("Child", realm, testAssets.scoped.id)
    testAssets.grandchild = createAsset("Grandchild", realm, testAssets.child.id)
    testAssets.sibling = createAsset("Sibling", realm, testAssets.ancestor.id)
    testAssets.unrelated = createAsset("Unrelated", realm)
    testAssets.otherRealm = createAsset("Other realm", keycloakSetup.realmCity.name)

    // Seed every target so a denied read cannot pass just because history is absent.
    testAssets.values().each { asset ->
      historicService.upsertValue(asset.id, ATTRIBUTE_NAME, 10d, timestamp)
      historicService.upsertValue(asset.id, ATTRIBUTE_NAME, 20d, timestamp.plusMinutes(1))
    }
  }

  def cleanup() {
    // Asset deletion cascades to historic datapoints. Include moved descendants and
    // skip the scope asset that the deleted-scope test has already removed.
    def remainingAssetIds = createdAssetIds.findAll { assetId ->
      assetStorageService.find(assetId, true) != null
    }
    if (!remainingAssetIds.isEmpty()) {
      assert assetStorageService.delete(remainingAssetIds)
    }
  }

  def "#scope scope reading #targetName history is allowed: #allowed"() {
    given:
    def facade = facadeFor(scope)
    def targetId = testAssets[targetName].id
    assert storedDatapoints(targetId) == originalDatapoints()

    when:
    def result = facade.getValueDatapoints(reference(targetId), query())

    then:
    snapshot(result) == (allowed ? originalDatapoints() : [:])
    storedDatapoints(targetId) == originalDatapoints()

    where:
    scope | targetName | allowed
    "global" | "scoped" | true
    "global" | "otherRealm" | true
    "realm" | "scoped" | true
    "realm" | "unrelated" | true
    "realm" | "otherRealm" | false
    "asset" | "scoped" | true
    "asset" | "child" | true
    "asset" | "grandchild" | true
    "asset" | "ancestor" | false
    "asset" | "sibling" | false
    "asset" | "unrelated" | false
    "asset" | "otherRealm" | false
  }

  def "#scope scope reading an attribute without history returns no datapoints"() {
    given:
    def target = createAsset("Without history", realm, testAssets.scoped.id)
    def facade = facadeFor(scope)
    assert storedDatapoints(target.id).isEmpty()

    when:
    def result = facade.getValueDatapoints(reference(target.id), query())

    then:
    result.length == 0

    where:
    scope << ["global", "realm", "asset"]
  }

  def "#scope scope reading a missing target returns no datapoints"() {
    given:
    def facade = facadeFor(scope)
    def missingId = UniqueIdentifierGenerator.generateId()
    assert assetStorageService.find(missingId, true) == null

    when:
    def result = facade.getValueDatapoints(reference(missingId), query())

    then:
    result.length == 0
    testAssets.values().every { storedDatapoints(it.id) == originalDatapoints() }

    where:
    scope << ["realm", "asset"]
  }

  def "Asset scope reading history fails when the scope asset has been deleted"() {
    given: "a facade whose scope asset exists at construction time"
    def scopeAsset = createAsset("Deleted scope", realm)
    def facade = new HistoricFacade(new RulesEngineId(realm, scopeAsset.id), assetStorageService, historicService)
    assert assetStorageService.delete([scopeAsset.id])
    assert assetStorageService.find(scopeAsset.id, true) == null

    when: "an existing target is accessed through the now invalid scope"
    facade.getValueDatapoints(reference(testAssets.unrelated.id), query())

    then: "the missing scope fails explicitly, as in the other scoped facades"
    thrown(IllegalStateException)
    storedDatapoints(testAssets.unrelated.id) == originalDatapoints()
  }

  def "Asset scope reading history is denied after a descendant moves outside the subtree"() {
    given:
    def facade = facadeFor("asset")
    def targetId = testAssets.grandchild.id
    assert snapshot(facade.getValueDatapoints(reference(targetId), query())) == originalDatapoints()

    and: "the target's parent is moved into another subtree in the same realm"
    def child = assetStorageService.find(testAssets.child.id, true)
    assetStorageService.merge(child.setParentId(testAssets.unrelated.id))
    def movedTarget = assetStorageService.find(targetId, true)
    assert movedTarget.realm == realm
    assert !movedTarget.path.contains(testAssets.scoped.id)
    assert movedTarget.path.contains(testAssets.unrelated.id)
    assert storedDatapoints(targetId) == originalDatapoints()

    when: "the same facade reads the target again"
    def result = facade.getValueDatapoints(reference(targetId), query())

    then:
    result.length == 0
    storedDatapoints(targetId) == originalDatapoints()
  }

  private ThingAsset createAsset(String name, String assetRealm, String parentId = null) {
    def asset = new ThingAsset("Historic facade ${name}")
            .setRealm(assetRealm)
            .setParentId(parentId)
    asset.addOrReplaceAttributes(new Attribute<>(ATTRIBUTE_NAME, ValueType.NUMBER))
    asset = assetStorageService.merge(asset)
    createdAssetIds.add(asset.id)
    return asset
  }

  private HistoricFacade facadeFor(String scope) {
    RulesEngineId engineId
    switch (scope) {
      case "global":
        engineId = new RulesEngineId()
        break
      case "realm":
        engineId = new RulesEngineId(realm)
        break
      case "asset":
        engineId = new RulesEngineId(realm, testAssets.scoped.id)
        break
      default:
        throw new IllegalArgumentException("Unknown scope: " + scope)
    }
    return new HistoricFacade(engineId, assetStorageService, historicService)
  }

  private static AttributeRef reference(String assetId) {
    return new AttributeRef(assetId, ATTRIBUTE_NAME)
  }

  private AssetDatapointAllQuery query() {
    return new AssetDatapointAllQuery(timestamp.minusMinutes(1), timestamp.plusMinutes(2))
  }

  private Map storedDatapoints(String assetId) {
    // Verify the seeded history independently of the facade under test.
    return snapshot(historicService.getDatapoints(reference(assetId)))
  }

  private static Map snapshot(def datapoints) {
    return datapoints.collectEntries { [(it.timestamp): it.value] }
  }

  private Map originalDatapoints() {
    return [(epochMillis(timestamp)): 10d, (epochMillis(timestamp.plusMinutes(1))): 20d]
  }

  private static long epochMillis(LocalDateTime time) {
    return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
  }
}
