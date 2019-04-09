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
package org.openremote.test.assets

import org.openremote.container.Container
import org.openremote.model.asset.AssetModelResource
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.asset.AssetType.AGENT
import static org.openremote.model.asset.AssetType.BUILDING
import static org.openremote.model.asset.AssetType.CONSOLE
import static org.openremote.model.asset.AssetType.CUSTOM
import static org.openremote.model.asset.AssetType.FLOOR
import static org.openremote.model.asset.AssetType.RESIDENCE
import static org.openremote.model.asset.AssetType.ROOM
import static org.openremote.model.asset.AssetType.THING

class AssetModelResourceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static Container container
    @Shared
    static AssetModelResource assetModelResource

    def setupSpec() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        container = startContainer(defaultConfig(serverPort), defaultServices())
        assetModelResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM).proxy(AssetModelResource.class)
    }

    def cleanupSpec() {
        given: "the server should be stopped"
        stopContainer(container)
    }

    def "Request types"() {

        when: "a request for Asset types is made"

        def assetDescriptors = assetModelResource.getAssetDescriptors(null)

        then: "the eight default types should be present"
        assetDescriptors.size() == 8
        assetDescriptors[0].name == CUSTOM.name
        assetDescriptors[1].name == BUILDING.name
        assetDescriptors[1].attributeDescriptors.get().length == 4
        assetDescriptors[1].attributeDescriptors.get()[0].valueDescriptor.name == ValueType.STRING
        assetDescriptors[2].name == FLOOR.name
        assetDescriptors[3].name == RESIDENCE.name
        assetDescriptors[4].name == ROOM.name
        assetDescriptors[5].name == AGENT.name
        assetDescriptors[6].name == CONSOLE.name
        assetDescriptors[7].name == THING.name

        when: "a request for Attribute types is made"

        def attributeTypeDescriptors = assetModelResource.getAttributeTypeDescriptors(null)

        then: "the eleven default types should be present"
        attributeTypeDescriptors.size() == 11
        attributeTypeDescriptors[0].name == "consoleName"
        attributeTypeDescriptors[1].name == "consoleVersion"
        attributeTypeDescriptors[2].name == "consolePlatform"
        attributeTypeDescriptors[3].name == "consoleProviders"
        attributeTypeDescriptors[4].name == "email"
        attributeTypeDescriptors[5].name == "city"
        attributeTypeDescriptors[6].name == "country"
        attributeTypeDescriptors[7].name == "postalCode"
        attributeTypeDescriptors[8].name == "street"
        attributeTypeDescriptors[9].name == "location"
        attributeTypeDescriptors[10].name == "surfaceArea"

        when: "a request for Attribute value types is made"

        def attributeValueTypeDescriptors = assetModelResource.getAttributeValueDescriptors(null)

        then: "the 50 default value types should be present"
        attributeValueTypeDescriptors.size() == 50

        when: "a request for Attribute value types is made"

        def metaItemDescriptors = assetModelResource.getMetaItemDescriptors(null)

        then: "the 26 default MetaItem types should be present"
        metaItemDescriptors.size() == 26
    }
}
