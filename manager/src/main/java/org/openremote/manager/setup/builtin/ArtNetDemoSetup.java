package org.openremote.manager.setup.builtin;

import org.openremote.agent.protocol.artnet.ArtnetClientProtocol;
import org.openremote.agent.protocol.simulator.SimulatorProtocol;
import org.openremote.container.Container;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.setup.Setup;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.security.Tenant;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;

import static org.openremote.model.asset.AssetType.*;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemType.*;

public class ArtNetDemoSetup implements Setup {

    @Override
    public void onInit() throws Exception
    {

    }

    @Override
    public void onStart() throws Exception
    {

    }

    //TODO FIND RIGHT PLACE FOR THIS CLASS
    public void setupTreeStructure(Tenant parentTenant, AssetStorageService assetStorageService, String agentProtocolConfigName) {

        //SETUP MAIN ARTNET-ASSET UNDER MASTER ASSET
        Asset artNetNetwork = new Asset();
        artNetNetwork.setRealm(parentTenant.getRealm());
        artNetNetwork.setName("ArtNet Network");
        artNetNetwork.setType(AGENT);
        artNetNetwork.addAttributes(
            initProtocolConfiguration(new AssetAttribute(agentProtocolConfigName), ArtnetClientProtocol.PROTOCOL_NAME)
                .addMeta(
                        new MetaItem(
                                ArtnetClientProtocol.META_PROTOCOL_HOST,
                                Values.create("127.0.0.1")
                        ),
                        new MetaItem(
                                ArtnetClientProtocol.META_PROTOCOL_PORT,
                                Values.create(6454)
                        )
                )
        );
        artNetNetwork = assetStorageService.merge(artNetNetwork);

        //SETUP UNIVERSE-ASSET UNDER MAIN ARTNET-ASSET
        Asset artNetUniverse = new Asset();
        artNetUniverse.setParent(artNetNetwork);
        artNetUniverse.setName("ArtNet Universe");
        artNetUniverse.setType(THING);
        List<AssetAttribute> artNetUniverseAttributes = Arrays.asList(
                new AssetAttribute("Values", OBJECT, Values.parseOrNull("{'universe': 0,'r': 0,'g': 0,'b': 0,'w': 0,'dim': 0}")).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(artNetNetwork.getId(), agentProtocolConfigName).toArrayValue())
                )
        );
        artNetUniverse.setAttributes(artNetUniverseAttributes);
        artNetUniverse = assetStorageService.merge(artNetUniverse);

        //SETUP LIGHT-ASSET UNDER UNIVERSE-ASSET
        Asset artNetLight = new Asset();
        artNetLight.setParent(artNetUniverse);
        artNetLight.setName("ArtNet Light");
        artNetLight.setType(THING);
        List<AssetAttribute> artNetLightAttributes = Arrays.asList(
            new AssetAttribute("Values", OBJECT, Values.parseOrNull("{'universe': 0,'r': 0,'g': 0,'b': 0,'w': 0,'dim': 0}")).addMeta(
                new MetaItem(AGENT_LINK, new AttributeRef(artNetNetwork.getId(), agentProtocolConfigName).toArrayValue())
            )
        );
        artNetLight.setAttributes(artNetLightAttributes);
        artNetLight = assetStorageService.merge(artNetLight);
    }

    public void discoverLights() {

    }

}
