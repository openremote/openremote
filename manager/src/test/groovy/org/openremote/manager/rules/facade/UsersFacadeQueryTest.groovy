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
package org.openremote.manager.rules.facade

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.rules.RulesEngineId
import org.openremote.manager.security.ManagerIdentityProvider
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.model.query.UserQuery
import org.openremote.model.query.filter.PathPredicate
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.security.User
import spock.lang.Specification

class UsersFacadeQueryTest extends Specification {

  def "Reusing a #scope query with #filterCase keeps system-account exclusion bounded"() {
    given: "the query object retained across rule firings"
    def callerFilters = createFilters() as UserQuery.AttributeValuePredicate[]
    def query = new UserQuery().attributes(callerFilters)
    def expectedFilters = snapshot(callerFilters)
    if (!alreadyExcluded) {
      expectedFilters.add(exclusion().toString())
    }
    List<List<String>> submittedFilters = []
    def provider = Stub(ManagerIdentityProvider) {
      queryUsers(_ as UserQuery) >> { UserQuery submitted ->
        // Snapshot at execution time; retaining the mutable query would hide earlier growth.
        submittedFilters.add(snapshot(submitted.attributes))
        new User[0]
      }
    }
    def identityService = Stub(ManagerIdentityService) {
      getIdentityProvider() >> provider
    }
    def engineId = scope == "global" ? new RulesEngineId() :
            scope == "realm" ? new RulesEngineId("building") : new RulesEngineId("building", "scope-asset")
    def facade = new UsersFacade(engineId, Stub(AssetStorageService), Stub(NotificationService), identityService)

    when: "the same query is executed repeatedly"
    100.times { facade.getResults(query).toList() }
    def filterCounts = submittedFilters.collect { it.size() }.unique()

    then: "every execution preserves the caller filters and has just one full system-account exclusion"
    submittedFilters.size() == 100
    filterCounts == [expectedFilters.size()]
    submittedFilters.each { assert it == expectedFilters }

    where:
    [scope, filterCase, createFilters, alreadyExcluded] << ["global", "realm", "asset"].collectMany { scope ->
      [
        [
          "no attributes",
          { -> null },
          false
        ],
        [
          "empty attributes",
          { -> [] },
          false
        ],
        [
          "a caller filter",
          { -> [department()] },
          false
        ],
        [
          "an existing full exclusion",
          { -> [department(), exclusion()] },
          true
        ],
        [
          "a value-specific exclusion",
          { -> [department(), exclusion("true")] },
          false
        ],
        [
          "a positive system filter",
          {
            -> [
              department(),
              new UserQuery.AttributeValuePredicate(false, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE))
            ]
          },
          false
        ]
      ].collect { row -> [scope] + row }
    }
  }

  def "Reusing an asset query with #pathCase keeps the scope path bounded"() {
    given: "an asset-scoped rule retaining its user query across firings"
    def query = new UserQuery().assetPath(callerPath)
    List<List<String>> submittedPaths = []
    def provider = Stub(ManagerIdentityProvider) {
      queryUsers(_ as UserQuery) >> { UserQuery submitted ->
        submittedPaths.add(submitted.pathPredicate.path.toList())
        new User[0]
      }
    }
    def identityService = Stub(ManagerIdentityService) {
      getIdentityProvider() >> provider
    }
    def facade = new UsersFacade(new RulesEngineId("building", "scope-asset"),
            Stub(AssetStorageService), Stub(NotificationService), identityService)

    when: "the same query is executed repeatedly"
    100.times { facade.getResults(query).toList() }
    def pathLengths = submittedPaths.collect { it.size() }.unique()

    then: "every execution retains the caller's path and includes the scope asset only once"
    submittedPaths.size() == 100
    pathLengths == [expectedPath.size()]
    submittedPaths.each { assert it == expectedPath }

    where:
    pathCase | callerPath | expectedPath
    "no predicate" | null | ["scope-asset"]
    "null path values" | new PathPredicate() | ["scope-asset"]
    "empty path values" | new PathPredicate([] as String[]) | ["scope-asset"]
    "a descendant filter" | new PathPredicate("child") | ["child", "scope-asset"]
    "an outside filter" | new PathPredicate("outside") | ["outside", "scope-asset"]
    "the scope already present" | new PathPredicate("scope-asset") | ["scope-asset"]
    "the scope at the start" | new PathPredicate("scope-asset", "child") | ["scope-asset", "child"]
    "the scope at the end" | new PathPredicate("child", "scope-asset") | ["child", "scope-asset"]
  }

  private static UserQuery.AttributeValuePredicate department() {
    new UserQuery.AttributeValuePredicate(false, new StringPredicate("department"), new StringPredicate("maintenance"))
  }

  private static UserQuery.AttributeValuePredicate exclusion(String value = null) {
    new UserQuery.AttributeValuePredicate(true, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE),
            value == null ? null : new StringPredicate(value))
  }

  private static List<String> snapshot(UserQuery.AttributeValuePredicate[] attributes) {
    attributes == null ? [] : attributes.collect { it.toString() }
  }
}
