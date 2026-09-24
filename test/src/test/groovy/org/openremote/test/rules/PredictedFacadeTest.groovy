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
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.rules.RulesEngineId
import org.openremote.manager.rules.facade.PredictedFacade
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

class PredictedFacadeTest extends Specification implements ManagerContainerTrait {

  private static final String ATTRIBUTE_NAME = "prediction"
  private static final String OTHER_ATTRIBUTE_NAME = "otherPrediction"
  private static final List<String> WRITE_OVERLOADS = ["asset ID", "attribute reference"]
  private static final List<String> PURGE_OPERATIONS = ["purgeValues", "purgeValuesBefore"]
  private static final List<String> OPERATIONS = ["read", *WRITE_OVERLOADS, *PURGE_OPERATIONS]

  AssetStorageService assetStorageService
  AssetPredictedDatapointService predictedService
  Map<String, ThingAsset> testAssets = [:]
  List<String> createdAssetIds = []
  String realm
  LocalDateTime timestamp

  def setup() {
    def container = startContainer(defaultConfig(), defaultServices())
    stopPseudoClock()
    assetStorageService = container.getService(AssetStorageService.class)
    predictedService = container.getService(AssetPredictedDatapointService.class)
    def keycloakSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
    realm = keycloakSetup.realmBuilding.name
    timestamp = getInstantTimeOf(container).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .plusDays(1).truncatedTo(ChronoUnit.SECONDS)

    // The scope is below an ancestor; siblings share that ancestor but are outside the scope.
    testAssets.ancestor = createAsset("Ancestor", realm)
    testAssets.scoped = createAsset("Scoped", realm, testAssets.ancestor.id)
    testAssets.child = createAsset("Child", realm, testAssets.scoped.id)
    testAssets.grandchild = createAsset("Grandchild", realm, testAssets.child.id)
    testAssets.sibling = createAsset("Sibling", realm, testAssets.ancestor.id)
    testAssets.unrelated = createAsset("Unrelated", realm)
    testAssets.otherRealm = createAsset("Other realm", keycloakSetup.realmCity.name)

    // Seed every target so that denied reads cannot pass simply because no predictions exist.
    testAssets.values().each { asset ->
      predictedService.upsertValue(asset.id, ATTRIBUTE_NAME, 10d, timestamp)
    }
  }

  def cleanup() {
    try {
      createdAssetIds.each { assetId ->
        predictedService.purgeValues(assetId, ATTRIBUTE_NAME)
        predictedService.purgeValues(assetId, OTHER_ATTRIBUTE_NAME)
      }
    } finally {
      // One test deletes its scope asset itself; delete the rest together so descendants
      // are included even when a test has moved them to another parent.
      def remainingAssetIds = createdAssetIds.findAll { assetId ->
        assetStorageService.find(assetId, true) != null
      }
      if (!remainingAssetIds.isEmpty()) {
        assert assetStorageService.delete(remainingAssetIds)
      }
    }
  }

  def "#scope scope reading #targetName predictions is allowed: #allowed"() {
    given:
    def facade = facadeFor(scope)
    def target = testAssets[targetName]

    when:
    def result = facade.getValueDatapoints(reference(target.id), query())

    then:
    snapshot(result) == (allowed ? originalDatapoints() : [:])
    storedDatapoints(target.id) == originalDatapoints()

    where:
    [scope, targetName, allowed] << scopeCases()
  }

  def "#scope scope writing #targetName predictions using #overload is allowed: #allowed"() {
    given:
    def facade = facadeFor(scope)
    def target = testAssets[targetName]

    when: "both an overwrite and an insertion are attempted"
    writePredictions(facade, target.id, overload)

    then:
    storedDatapoints(target.id) == (allowed ? updatedDatapoints() : originalDatapoints())

    where:
    [scope, targetName, allowed, overload] << scopeCases().collectMany { row ->
      WRITE_OVERLOADS.collect { overload -> row + [overload] }
    }
  }

  def "#scope scope #operation on #targetName predictions is allowed: #allowed"() {
    given: "predictions before, exactly at, and one millisecond after the cutoff"
    def facade = facadeFor(scope)
    def target = testAssets[targetName]
    target.addOrReplaceAttributes(new Attribute<>(OTHER_ATTRIBUTE_NAME, ValueType.NUMBER))
    assetStorageService.merge(target)
    purgeDatapoints().each { time, value ->
      predictedService.upsertValue(target.id, ATTRIBUTE_NAME, value, time)
      predictedService.upsertValue(target.id, OTHER_ATTRIBUTE_NAME, value, time)
    }
    def expected = allowed ? (operation == "purgeValues" ? [:] : retainedDatapoints()) : purgeDatapoints()

    when:
    performOperation(facade, target.id, operation)

    then: "only the permitted predictions of the requested attribute are removed"
    storedDatapoints(target.id) == expected
    snapshot(predictedService.getDatapoints(new AttributeRef(target.id, OTHER_ATTRIBUTE_NAME))) == purgeDatapoints()
    testAssets.values().findAll { it.id != target.id }.every {
      storedDatapoints(it.id) == originalDatapoints()
    }

    when: "the same purge is repeated"
    performOperation(facade, target.id, operation)

    then: "the remaining predictions are unchanged"
    storedDatapoints(target.id) == expected
    snapshot(predictedService.getDatapoints(new AttributeRef(target.id, OTHER_ATTRIBUTE_NAME))) == purgeDatapoints()
    testAssets.values().findAll { it.id != target.id }.every {
      storedDatapoints(it.id) == originalDatapoints()
    }

    where:
    [scope, targetName, allowed, operation] << scopeCases().collectMany { row ->
      PURGE_OPERATIONS.collect { operation -> row + [operation] }
    }
  }

  def "#scope scope #operation on an attribute without predictions is a no-op"() {
    given:
    def facade = facadeFor(scope)
    def targetId = testAssets.scoped.id
    predictedService.purgeValues(targetId, ATTRIBUTE_NAME)
    assert storedDatapoints(targetId).isEmpty()

    when:
    performOperation(facade, targetId, operation)

    then:
    notThrown(Exception)
    storedDatapoints(targetId).isEmpty()
    testAssets.values().findAll { it.id != targetId }.every {
      storedDatapoints(it.id) == originalDatapoints()
    }

    where:
    [scope, operation] << ["global", "realm", "asset"].collectMany { scope ->
      PURGE_OPERATIONS.collect { operation -> [scope, operation] }
    }
  }

  def "#scope scope #operation on a missing target is denied without creating predictions"() {
    given:
    def facade = facadeFor(scope)
    def missingId = UniqueIdentifierGenerator.generateId()
    assert assetStorageService.find(missingId, true) == null

    when:
    def result = performOperation(facade, missingId, operation)

    then:
    notThrown(Exception)
    operation != "read" || result.length == 0
    predictedService.getDatapoints(reference(missingId)).isEmpty()
    testAssets.values().every { storedDatapoints(it.id) == originalDatapoints() }

    cleanup: "remove orphan predictions written by the currently unchecked facade"
    predictedService.purgeValues(missingId, ATTRIBUTE_NAME)

    where:
    [scope, operation] << ["realm", "asset"].collectMany { scope ->
      OPERATIONS.collect { operation -> [scope, operation] }
    }
  }

  def "Asset scope #operation fails when the scope asset has been deleted"() {
    given: "a facade whose scope asset exists at construction time"
    def scopeAsset = createAsset("Deleted scope", realm)
    def facade = new PredictedFacade(new RulesEngineId(realm, scopeAsset.id), assetStorageService, predictedService)
    assert assetStorageService.delete([scopeAsset.id])
    assert assetStorageService.find(scopeAsset.id, true) == null

    when: "an existing target is accessed through the now invalid scope"
    performOperation(facade, testAssets.unrelated.id, operation)

    then: "the missing scope fails explicitly, as it does in AssetsFacade"
    thrown(IllegalStateException)
    storedDatapoints(testAssets.unrelated.id) == originalDatapoints()

    where:
    operation << OPERATIONS
  }

  def "Asset scope #operation is denied after a descendant moves outside the subtree"() {
    given:
    def facade = facadeFor("asset")
    def targetId = testAssets.grandchild.id

    and: "the operation succeeds while the target is a descendant"
    def initialResult = performOperation(facade, targetId, operation)
    assert operation != "read" || snapshot(initialResult) == originalDatapoints()
    def expectedDatapoints = originalDatapoints()
    if (WRITE_OVERLOADS.contains(operation)) {
      expectedDatapoints = updatedDatapoints()
    } else if (PURGE_OPERATIONS.contains(operation)) {
      expectedDatapoints = [:]
    }
    assert storedDatapoints(targetId) == expectedDatapoints

    and: "the target's parent is moved into another subtree in the same realm"
    // Restore the baseline so that a forbidden write or purge would be observable.
    predictedService.purgeValues(targetId, ATTRIBUTE_NAME)
    predictedService.upsertValue(targetId, ATTRIBUTE_NAME, 10d, timestamp)
    def child = assetStorageService.find(testAssets.child.id, true)
    assetStorageService.merge(child.setParentId(testAssets.unrelated.id))
    def movedTarget = assetStorageService.find(targetId, true)
    assert movedTarget.realm == realm
    assert !movedTarget.path.contains(testAssets.scoped.id)
    assert movedTarget.path.contains(testAssets.unrelated.id)

    when: "the same facade attempts the operation again"
    def result = performOperation(facade, targetId, operation)

    then:
    operation != "read" || result.length == 0
    storedDatapoints(targetId) == originalDatapoints()

    where:
    operation << OPERATIONS
  }

  private ThingAsset createAsset(String name, String assetRealm, String parentId = null) {
    def asset = new ThingAsset("Predicted facade ${name}")
            .setRealm(assetRealm)
            .setParentId(parentId)
    asset.addOrReplaceAttributes(new Attribute<>(ATTRIBUTE_NAME, ValueType.NUMBER))
    asset = assetStorageService.merge(asset)
    createdAssetIds.add(asset.id)
    return asset
  }

  private PredictedFacade facadeFor(String scope) {
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
    return new PredictedFacade(engineId, assetStorageService, predictedService)
  }

  private Object performOperation(PredictedFacade facade, String assetId, String operation) {
    if (operation == "read") {
      return facade.getValueDatapoints(reference(assetId), query())
    }
    if (operation == "purgeValues") {
      facade.purgeValues(assetId, ATTRIBUTE_NAME)
    } else if (operation == "purgeValuesBefore") {
      facade.purgeValuesBefore(assetId, ATTRIBUTE_NAME, timestamp.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant())
    } else {
      writePredictions(facade, assetId, operation)
    }
    return null
  }

  private void writePredictions(PredictedFacade facade, String assetId, String overload) {
    if (overload == "asset ID") {
      facade.updateValue(assetId, ATTRIBUTE_NAME, 20d, timestamp)
      facade.updateValue(assetId, ATTRIBUTE_NAME, 30d, timestamp.plusMinutes(1))
    } else if (overload == "attribute reference") {
      facade.updateValue(reference(assetId), 20d, timestamp)
      facade.updateValue(reference(assetId), 30d, timestamp.plusMinutes(1))
    } else {
      throw new IllegalArgumentException("Unknown overload: " + overload)
    }
  }

  private static AttributeRef reference(String assetId) {
    return new AttributeRef(assetId, ATTRIBUTE_NAME)
  }

  private AssetDatapointAllQuery query() {
    return new AssetDatapointAllQuery(timestamp.minusMinutes(1), timestamp.plusMinutes(2))
  }

  private Map storedDatapoints(String assetId) {
    // Verify persistence directly, independently of the facade being tested.
    return snapshot(predictedService.getDatapoints(reference(assetId)))
  }

  private static Map snapshot(def datapoints) {
    return datapoints.collectEntries { [(it.timestamp): it.value] }
  }

  private Map originalDatapoints() {
    return [(epochMillis(timestamp)): 10d]
  }

  private Map updatedDatapoints() {
    return [(epochMillis(timestamp)): 20d, (epochMillis(timestamp.plusMinutes(1))): 30d]
  }

  private Map retainedDatapoints() {
    long cutoff = epochMillis(timestamp.plusMinutes(1))
    return [(cutoff): 20d, (cutoff + 1): 30d]
  }

  private Map purgeDatapoints() {
    return originalDatapoints() + retainedDatapoints()
  }

  private static long epochMillis(LocalDateTime time) {
    return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
  }

  private static List<List> scopeCases() {
    return [
      ["global", "scoped", true],
      ["global", "otherRealm", true],
      ["realm", "scoped", true],
      ["realm", "unrelated", true],
      ["realm", "otherRealm", false],
      ["asset", "scoped", true],
      ["asset", "child", true],
      ["asset", "grandchild", true],
      ["asset", "ancestor", false],
      ["asset", "sibling", false],
      ["asset", "unrelated", false],
      ["asset", "otherRealm", false]
    ]
  }
}
