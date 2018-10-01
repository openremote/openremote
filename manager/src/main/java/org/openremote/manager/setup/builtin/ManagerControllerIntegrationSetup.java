package org.openremote.manager.setup.builtin;

import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;

import org.openremote.agent.protocol.controller.ControllerProtocol;
import org.openremote.container.Container;
import org.openremote.manager.setup.AbstractManagerSetup;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.security.Tenant;
import org.openremote.model.value.Values;

/**
 * ManagerControllerIntegrationSetup use HttpClientProtocol to communicate with openremote Controller 2 REST API.
 *
 * First step is to read information from sensor (READ) : OK.
 * Next step is to send command (WRITE) : Still confusing how to write only?.
 * Next step is to consider READ/WRITE
 *
 * <p>
 * Date : 09-Aug-18
 *
 * @author jerome.vervier
 */
public class ManagerControllerIntegrationSetup extends AbstractManagerSetup {
   private final String baseUrl = "http://10.129.15.70:8688/controller";

    public String masterRealmId;

    public ManagerControllerIntegrationSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        KeycloakDemoSetup keycloakDemoSetup = setupService.getTaskOfType(KeycloakDemoSetup.class);
        Tenant masterTenant = keycloakDemoSetup.masterTenant;
        masterRealmId = masterTenant.getId();

        Asset building = new Asset("Building", AssetType.BUILDING);
        building.setRealmId(masterRealmId);
        building = assetStorageService.merge(building);

        Asset controllerAgent = new Asset("Controller Agent", AGENT, building);
        controllerAgent.setAttributes(
                initProtocolConfiguration(new AssetAttribute("controllerConfig"), ControllerProtocol.PROTOCOL_NAME)
                        .addMeta(
                                new MetaItem(
                                        ControllerProtocol.META_PROTOCOL_BASE_URI,
                                        Values.create(baseUrl)
                                )
                        )
        );

        controllerAgent = assetStorageService.merge(controllerAgent);

        building = assetStorageService.merge(building);

        /*Asset deviceDay = new Asset("Device date", AssetType.THING, building);

        AssetAttribute dayTime = new AssetAttribute("CurrentDateTime", STRING);
        dayTime.addMeta(
                AgentLink.asAgentLinkMetaItem(controllerAgent.getAttribute("controllerConfig").get().getReferenceOrThrow()),
                new MetaItem(ControllerProtocol.META_ATTRIBUTE_DEVICE_NAME, Values.create("DateTime")),
                new MetaItem(ControllerProtocol.META_ATTRIBUTE_SENSOR_NAME, Values.create("DisplayDate_Sensor")),
                new MetaItem(AssetMeta.READ_ONLY, Values.create(true))
        );

        deviceDay.addAttributes(dayTime);

        Asset writeCommand = new Asset("Exec Google Command", AssetType.THING, building);

        AssetAttribute callGoogleCommand = new AssetAttribute("callGoogleCommand", NUMBER);
        callGoogleCommand.addMeta(
                AgentLink.asAgentLinkMetaItem(controllerAgent.getAttribute("controllerConfig").get().getReferenceOrThrow()),
                new MetaItem(ControllerProtocol.META_ATTRIBUTE_DEVICE_NAME, Values.create("DateTime")),
                new MetaItem(ControllerProtocol.META_ATTRIBUTE_COMMAND_NAME, Values.create("callGoogleCommand"))
        );

        writeCommand.addAttributes(callGoogleCommand);

        writeCommand = assetStorageService.merge(writeCommand);

        deviceDay = assetStorageService.merge(deviceDay);

        controllerAgent = assetStorageService.merge(controllerAgent);

        building = assetStorageService.merge(building);*/
    }
}
