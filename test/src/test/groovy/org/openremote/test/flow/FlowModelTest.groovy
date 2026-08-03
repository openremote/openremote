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
package org.openremote.test.flow

import org.openremote.model.flow.Flow
import org.openremote.model.flow.Node
import org.openremote.model.flow.Wire
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

class FlowModelTest extends Specification implements ManagerContainerTrait {

    def "Ignore duplicate wires"() {

        given: "a flow"
        Flow flow = new Flow("Test Flow", "123")

        when: "duplicate wires are added"
        flow.addWire(new Wire("a", "b"))
        flow.addWire(new Wire("a", "b"))

        then: "only one wire should be present"
        flow.getWires().length == 1

    }

    def "Throw when duplicate constructor wires"() {

        when: "duplicate wires are provided as constructor arguments"
        new Flow("foo", "123", new Node[0], [new Wire("a", "b"), new Wire("a", "b")] as Wire[])

        then: "an exception should be thrown"
        thrown(IllegalArgumentException)
    }

}
