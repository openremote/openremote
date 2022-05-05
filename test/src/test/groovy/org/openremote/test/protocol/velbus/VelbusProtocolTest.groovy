package org.openremote.test.protocol.velbus

import org.apache.commons.io.IOUtils
import org.openremote.agent.protocol.velbus.VelbusAgentLink
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.agent.AgentResource
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.file.FileInfo
import org.openremote.model.util.TextUtil
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.stream.Collectors

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.TEXT

class VelbusProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    static def MockPackets = [
        // Module Type request for address 48 - return VMBGPOD Packets
        "0F FB 30 40 86 04 00 00 00 00 00 00 00 00": [
            "0F FB 30 07 FF 28 00 02 01 16 12 6D 04 00",
            "0F FB 30 08 B0 28 00 02 31 32 FF 40 42 04"
            //"0F FB 30 08 B0 28 00 02 31 32 33 40 0E 04"
        ],

        // Module Status request for address 48
        "0F FB 30 02 FA 00 CA 04 00 00 00 00 00 00": [
            "0F FB 30 07 ED 00 FF FF 00 00 8D 47 04 00",
            "0F FB 31 07 ED 00 FF FF 00 10 8D 36 04 00",
            "0F FB 32 07 ED 00 FF FF 00 00 8D 45 04 00",
        ],

        // Thermostat Status request for address 48
        "0F FB 30 02 E7 00 DD 04 00 00 00 00 00 00": [
            "0F FB 30 08 EA 00 00 10 38 18 00 00 74 04",
            "0F FB 30 08 E8 18 32 2C 20 18 06 01 21 04",
            "0F FB 30 08 E9 2A 2E 34 48 00 3C 05 C0 04"
        ]
    ]

    def "Check VELBUS agent and device asset deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a mock VELBUS agent is created"
        def agent = new MockVelbusAgent("VELBUS")
        agent.setRealm(MASTER_REALM)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((MockVelbusProtocol)agentService.getProtocolInstance(agent.id)).messageProcessor != null
        }

        when: "the mock packets are assigned to the mock protocol"
        ((MockVelbusProtocol)agentService.getProtocolInstance(agent.id)).messageProcessor.mockPackets = MockPackets

        and: "a device asset is created"
        def device = new ThingAsset("VELBUS Demo VMBGPOD")
            .setRealm(MASTER_REALM)
            .addOrReplaceAttributes(
                new Attribute<>("ch1State", TEXT)
                    .addOrReplaceMeta(
                        new MetaItem<>(
                                AGENT_LINK,
                                new VelbusAgentLink(agent.id, 48, "CH1")
                        )
                    )
            )

        and: "the device asset is added to the asset service"
        device = assetStorageService.merge(device)
        def deviceId = device.getId()

        then: "a client should be created and the device asset attribute values should match the values returned by the actual device"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((MockVelbusProtocol)agentService.getProtocolInstance(agent.id)).network != null
            def asset = assetStorageService.find(deviceId, true)
            assert asset.getAttribute("ch1State").flatMap { it.getValue() }.orElse(null) == "RELEASED"
        }

        cleanup: "remove agent"
        if (agent != null) {
            assetStorageService.delete([agent.id])
        }
    }

    def "Check linked attribute import"() {

        given: "the server container is started"
        def conditions = new PollingConditions(timeout: 60, delay: 0.5)
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "a VELBUS project file"
        def velbusProjectFileResource = getClass().getResourceAsStream(
            "/org/openremote/test/protocol/velbus/VelbusProject.vlp"
        )
        def velbusProjectFile = IOUtils.toString(velbusProjectFileResource, "UTF-8")

        and: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the agent resource"
        def agentResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AgentResource.class)

        expect: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a VELBUS agent is created"
        def agent = new MockVelbusAgent("VELBUS")
            .setRealm(MASTER_REALM)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((MockVelbusProtocol)agentService.getProtocolInstance(agent.id)).messageProcessor != null
        }

        when: "discovery is requested with a VELBUS project file"
        def fileInfo = new FileInfo("VelbusProject.vlp", velbusProjectFile, false)
        def assets = agentResource.doProtocolAssetImport(null, agent.getId(), MASTER_REALM, fileInfo)

        then: "the correct number of assets should be returned and all should have IDs"
        assert assets != null
        assert assets.length == 13
        assert assets.each {
            !TextUtil.isNullOrEmpty(it.asset.id) &&
                !TextUtil.isNullOrEmpty(it.asset.getName()) &&
                !it.asset.getAttributes().isEmpty() &&
                it.asset.getAttributes().values().every {attr ->
                    attr.getMetaValue(AGENT_LINK).map{
                        ValueUtil.getValue(it, VelbusAgentLink.class)
                                .map({agentLink -> agentLink.id == agent.id && agentLink.deviceAddress.isPresent() && agentLink.deviceValueLink.isPresent()})
                                .orElse(false)
                    }.orElse(true)
                }
        }

        and: "a given asset should have the correct attributes (VMBGPOD)"
        def asset = assets.find {it.asset.name == "VMBGPOD"}
        assert asset != null
        assert asset.asset.getAttributes().size() == 305
        def memoTextAttribute = asset.asset.getAttributes().values().find {it.getMetaValue(AGENT_LINK).map{(it as VelbusAgentLink).deviceValueLink.orElse(null) == "MEMO_TEXT"}.orElse(false)}
        assert memoTextAttribute != null
        assert memoTextAttribute.getMetaValue(AGENT_LINK).flatMap(){(it as VelbusAgentLink).deviceAddress}.orElse(null) == 24

        and: "all imported assets should be fully linked"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id).linkedAttributes.size() == 1163
        }

        when: "agent and imported assets are removed"
        def ids = []
        ids.addAll(Arrays.stream(assets).map{it.asset.id}.collect(Collectors.toList()))
        ids.add(agent.id)
        assetStorageService.delete(ids)

        then: "the agent and assets should not exist"
        conditions.eventually {
            assert assetStorageService.findNames(ids as String[]).isEmpty()
        }

        cleanup: "remove agent and imported assets"
        if (agent != null) {
            ids = []
            ids.addAll(Arrays.stream(assets).map{it.asset.id}.collect(Collectors.toList()))
            ids.add(agent.id)
            assetStorageService.delete(ids)
        }
    }
}
