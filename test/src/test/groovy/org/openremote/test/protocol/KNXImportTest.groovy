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
package org.openremote.test.protocol

import org.openremote.agent.protocol.knx.KNXAgent
import org.openremote.agent.protocol.knx.KNXProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.agent.AgentResource
import org.openremote.model.file.FileInfo
import org.openremote.model.util.TextUtil
import org.openremote.model.value.MetaItemType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

/**
 * This tests the KNX protocol import routine
 */
// Doesn't work when run via gradle
@Ignore
class KNXImportTest extends Specification implements ManagerContainerTrait {

    def "Check KNX protocol import"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        when: "a KNX agent is created. to test the import functionality"
        def knxAgent = new KNXAgent("KNX agent")
        knxAgent.setRealm(MASTER_REALM)
        knxAgent = assetStorageService.merge(knxAgent)

        then: "the protocol instance should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(knxAgent.getId()) != null
        }

        when: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token
        
        and: "the agent resource"
        def agentResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AgentResource.class)

        then: "the container should settle down and the agent should be deployed"
        conditions.eventually {
            assert agentService.getAgents().containsKey(knxAgent.id)
            assert noEventProcessedIn(assetProcessingService, 500)
        }
        
        when: "discovery is requested with a ETS project file"
        def knxProjectFileResource = getClass().getResourceAsStream(
            "/org/openremote/test/protocol/knx/knx-import-testproject.knxproj"
        )
        String base64Content = Base64.getEncoder().encodeToString(knxProjectFileResource.bytes)
        def fileInfo = new FileInfo("knx-import-testproject.knxproj", base64Content, true)
        def assets = agentResource.doProtocolAssetImport(null, knxAgent.getId(), null, fileInfo)
        
        then: "the new things and attributes should be created"
        conditions.eventually {
            assert assets != null
            assert assets.length == 12
            assert assets.each {
                !TextUtil.isNullOrEmpty(it.asset.id) &&
                    !TextUtil.isNullOrEmpty(it.asset.getName()) &&
                    !it.asset.attributes().isEmpty() &&
                    it.asset.getAttributes().stream().allMatch({attr ->
                        attr.getMetaValue(AGENT_LINK).map({agentLink -> agentLink.id == knxAgent.id})
                            .orElse(false)
                    })
    
            }
        }
        
        and: "a given asset should have the correct attributes (Target Temperature)"
        def asset = assets.find {it.asset.name == "Target Temperature"}
        assert asset != null
        assert asset.asset.getAttributes().size() == 1
        def attribute = asset.asset.getAttribute("TargetTemperature").get()
        assert attribute != null
        def metaItem = attribute.getMetaItem(KNXProtocol.META_KNX_STATUS_GA).get()
        assert metaItem != null
        assert metaItem.getValue().get() == "5/0/4"
        def metaItem2 = attribute.getMetaItem(KNXProtocol.META_KNX_ACTION_GA).get()
        metaItem2 != null
        metaItem2.getValue().get() == "5/0/0"
        def metaItem3 = attribute.getMetaItem(KNXProtocol.META_KNX_DPT).get()
        metaItem3 != null
        metaItem3.getValue().get() == "9.001"
    }
}
