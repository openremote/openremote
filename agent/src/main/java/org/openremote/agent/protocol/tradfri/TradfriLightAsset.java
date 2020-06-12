package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.event.*;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.HashMap;
import java.util.Optional;

import static org.openremote.model.attribute.AttributeValueType.NUMBER;
import static org.openremote.model.attribute.MetaItemType.*;

public class TradfriLightAsset extends TradfriAsset {

    /**
     * Construct the TradfriLightAsset class
     */
    public TradfriLightAsset(AssetAttribute protocolConfiguration, MetaItem agentLink, ProtocolAssetService assetService, Light light, HashMap<String, Device> tradfriDevices) {
        super(protocolConfiguration, agentLink, assetService, light, AssetType.LIGHT, tradfriDevices);
    }

    @Override
    public void createAssetAttributes() {
        asset.addAttributes(new AssetAttribute("colorTemperature", NUMBER, Values.create(0)));
        Optional<AssetAttribute> lightDimLevel = asset.getAttribute("lightDimLevel");
        if (lightDimLevel.isPresent()) {
            lightDimLevel.get().setType(NUMBER);
            lightDimLevel.get().setMeta(
                    new MetaItem(RANGE_MIN, Values.create(0)),
                    new MetaItem(RANGE_MAX, Values.create(254)),
                    new MetaItem(LABEL, Values.create("Brightness (0 - 254)")),
                    new MetaItem(DESCRIPTION, Values.create("The brightness of the TRÅDFRI light (Only for dimmable lights)")),
                    new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                    new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                    agentLink
            );
        }
        Optional<AssetAttribute> lightStatus = asset.getAttribute("lightStatus");
        if (lightStatus.isPresent()) lightStatus.get().setMeta(
                new MetaItem(LABEL, Values.create("State (on/off)")),
                new MetaItem(DESCRIPTION, Values.create("The state of the TRÅDFRI light (Checked means on, unchecked means off)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                agentLink
        );
        Optional<AssetAttribute> colorGBW = asset.getAttribute("colorGBW");
        if (colorGBW.isPresent()) colorGBW.get().setMeta(
                new MetaItem(LABEL, Values.create("Color")),
                new MetaItem(DESCRIPTION, Values.create("The color of the TRÅDFRI light (Only for RGB lights)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                agentLink
        );
        Optional<AssetAttribute> colorTemperature = asset.getAttribute("colorTemperature");
        if(colorTemperature.isPresent()) colorTemperature.get().setMeta(
                new MetaItem(RANGE_MIN, Values.create(250)),
                new MetaItem(RANGE_MAX, Values.create(454)),
                new MetaItem(LABEL, Values.create("Color temperature")),
                new MetaItem(DESCRIPTION, Values.create("The color temperature of the TRÅDFRI light (Only for white spectrum lights)")),
                new MetaItem(ACCESS_RESTRICTED_READ, Values.create(true)),
                new MetaItem(ACCESS_RESTRICTED_WRITE, Values.create(true)),
                agentLink
        );
    }

    @Override
    public void createEventHandlers() {
        EventHandler<LightChangeOnEvent> lightOnOffEventHandler = new EventHandler<LightChangeOnEvent>() {
            @Override
            public void handle(LightChangeOnEvent event) {
                Optional<AssetAttribute> lightStatus = asset.getAttribute("lightStatus");
                if(lightStatus.isPresent()) lightStatus.get().setValue(Values.create(device.toLight().getOn()));
                Optional<String> assetId = protocolConfiguration.getAssetId();
                if (assetId.isPresent()) asset.setRealm(assetService.findAsset(assetId.get()).getRealm());
                assetService.mergeAsset(asset);
            }
        };

        EventHandler<LightChangeBrightnessEvent> lightBrightnessEventHandler = new EventHandler<LightChangeBrightnessEvent>() {
            @Override
            public void handle(LightChangeBrightnessEvent event) {
                Optional<AssetAttribute> lightDimLevel = asset.getAttribute("lightDimLevel");
                if (lightDimLevel.isPresent()) lightDimLevel.get().setValue(Values.create(device.toLight().getBrightness()));
                Optional<String> assetId = protocolConfiguration.getAssetId();
                if (assetId.isPresent()) asset.setRealm(assetService.findAsset(assetId.get()).getRealm());
                assetService.mergeAsset(asset);
            }
        };

        EventHandler<LightChangeColourEvent> lightColourChangeEventHandler = new EventHandler<LightChangeColourEvent>() {
            @Override
            public void handle(LightChangeColourEvent event) {
                Optional<AssetAttribute> colorGBW = asset.getAttribute("colorGBW");
                if (colorGBW.isPresent()) colorGBW.get().setValue(Values.createObject().put("red", device.toLight().getColourRGB().getRed()).put("green", device.toLight().getColourRGB().getGreen()).put("blue", device.toLight().getColourRGB().getBlue()));
                Optional<String> assetId = protocolConfiguration.getAssetId();
                if (assetId.isPresent()) asset.setRealm(assetService.findAsset(assetId.get()).getRealm());
                assetService.mergeAsset(asset);
            }
        };

        EventHandler<LightChangeColourTemperatureEvent> lightColorTemperatureEventHandler = new EventHandler<LightChangeColourTemperatureEvent>() {
            @Override
            public void handle(LightChangeColourTemperatureEvent event) {
                Optional<AssetAttribute> colorTemperature = asset.getAttribute("colorTemperature");
                if (colorTemperature.isPresent()) colorTemperature.get().setValue(Values.create(device.toLight().getColourTemperature()));
                Optional<String> assetId = protocolConfiguration.getAssetId();
                if (assetId.isPresent()) asset.setRealm(assetService.findAsset(assetId.get()).getRealm());
                assetService.mergeAsset(asset);
            }
        };
        device.toLight().addEventHandler(lightOnOffEventHandler);
        device.toLight().addEventHandler(lightBrightnessEventHandler);
        device.toLight().addEventHandler(lightColourChangeEventHandler);
        device.toLight().addEventHandler(lightColorTemperatureEventHandler);
    }

    @Override
    public void setInitialValues() {
        Optional<AssetAttribute> lightStatus = asset.getAttribute("lightStatus");
        if(lightStatus.isPresent()) lightStatus.get().setValue(Values.create(device.toLight().getOn()));
        Optional<AssetAttribute> lightDimLevel = asset.getAttribute("lightDimLevel");
        if(lightDimLevel.isPresent()) lightDimLevel.get().setValue(Values.create(device.toLight().getBrightness()));
        Optional<AssetAttribute> colorGBW = asset.getAttribute("colorGBW");
        if(colorGBW.isPresent()) colorGBW.get().setValue(Values.createObject().put("red", device.toLight().getColourRGB().getRed()).put("green", device.toLight().getColourRGB().getGreen()).put("blue", device.toLight().getColourRGB().getBlue()));
        Optional<AssetAttribute> colorTemperature = asset.getAttribute("colorTemperature");
        if(colorTemperature.isPresent()) colorTemperature.get().setValue(Values.create(device.toLight().getColourTemperature()));
    }
}
