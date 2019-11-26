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
import org.openremote.model.attribute.AttributeType
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.asset.AssetType.AGENT
import static org.openremote.model.asset.AssetType.AREA
import static org.openremote.model.asset.AssetType.BUILDING
import static org.openremote.model.asset.AssetType.CAMERA
import static org.openremote.model.asset.AssetType.CITY
import static org.openremote.model.asset.AssetType.CONSOLE
import static org.openremote.model.asset.AssetType.ENVIRONMENT_SENSOR
import static org.openremote.model.asset.AssetType.FLOOR
import static org.openremote.model.asset.AssetType.LIGHT
import static org.openremote.model.asset.AssetType.MICROPHONE
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

        then: "the default asset types should be present"
        assetDescriptors.size() == 13
        assetDescriptors[0].name == BUILDING.name
        assetDescriptors[0].attributeDescriptors.length == 5
        assetDescriptors[0].attributeDescriptors.find {it.attributeName == AttributeType.SURFACE_AREA.attributeName}.valueDescriptor.valueType == ValueType.NUMBER
        assetDescriptors[1].name == CITY.name
        assetDescriptors[2].name == AREA.name
        assetDescriptors[3].name == FLOOR.name
        assetDescriptors[4].name == RESIDENCE.name
        assetDescriptors[5].name == ROOM.name
        assetDescriptors[6].name == AGENT.name
        assetDescriptors[7].name == CONSOLE.name
        assetDescriptors[8].name == MICROPHONE.name
        assetDescriptors[9].name == ENVIRONMENT_SENSOR.name
        assetDescriptors[10].name == LIGHT.name
        assetDescriptors[11].name == CAMERA.name
        assetDescriptors[12].name == THING.name

        when: "a request for Attribute types is made"

        def attributeTypeDescriptors = assetModelResource.getAttributeDescriptors(null)

        then: "the default types should be present"
        attributeTypeDescriptors.size() == 13
        attributeTypeDescriptors.any {it.attributeName == "consoleName"}
        attributeTypeDescriptors.any {it.attributeName == "consoleVersion"}
        attributeTypeDescriptors.any {it.attributeName == "consolePlatform"}
        attributeTypeDescriptors.any {it.attributeName == "consoleProviders"}
        attributeTypeDescriptors.any {it.attributeName == "email"}
        attributeTypeDescriptors.any {it.attributeName == "city"}
        attributeTypeDescriptors.any {it.attributeName == "country"}
        attributeTypeDescriptors.any {it.attributeName == "postalCode"}
        attributeTypeDescriptors.any {it.attributeName == "street"}
        attributeTypeDescriptors.any {it.attributeName == "location"}
        attributeTypeDescriptors.any {it.attributeName == "surfaceArea"}
        attributeTypeDescriptors.any {it.attributeName == "assetStatus"}
        attributeTypeDescriptors.any {it.attributeName == "assetTags"}

        when: "a request for Attribute value types is made"

        def attributeValueTypeDescriptors = assetModelResource.getAttributeValueDescriptors(null)

        then: "the default value types should be present"
        attributeValueTypeDescriptors.size() == 56

        when: "a request for Attribute value types is made"

        def metaItemDescriptors = assetModelResource.getMetaItemDescriptors(null)

        then: "the default MetaItem types should be present"
        metaItemDescriptors.size() == MetaItemType.values().length
    }
}
