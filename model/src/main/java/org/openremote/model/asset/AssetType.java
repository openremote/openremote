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
package org.openremote.model.asset;

import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.attribute.AttributeDescriptorImpl;
import org.openremote.model.attribute.MetaItemDescriptorImpl;
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.attribute.AttributeType.*;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemType.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains the well-known URIs for functionality we want
 * to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType implements AssetDescriptor {

    BUILDING(ASSET_NAMESPACE + ":building", "office-building", "4b5966",
        SURFACE_AREA,
        GEO_STREET,
        GEO_CITY,
        GEO_COUNTRY,
        GEO_POSTAL_CODE),

    CITY(ASSET_NAMESPACE + ":city", "city", null,
        GEO_CITY,
        GEO_COUNTRY),

    AREA(ASSET_NAMESPACE + ":area", "home-city", "095F6B",
        GEO_CITY,
        GEO_COUNTRY,
        GEO_POSTAL_CODE),

    FLOOR(ASSET_NAMESPACE + ":floor", "stairs", null),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "home", "4c879b"),

    ROOM(ASSET_NAMESPACE + ":room", "door", "2eaaa2"),

    AGENT(ASSET_NAMESPACE + ":agent", "cogs", null),

    CONSOLE(ASSET_NAMESPACE + ":console", "monitor-cellphone", null),

    MICROPHONE(ASSET_NAMESPACE + ":microphone", "microphone", "47A5FF",
        new AttributeDescriptorImpl("microphoneLevel", SOUND, Values.create(0),
            LABEL.withInitialValue(Values.create("Microphone Level")),
            DESCRIPTION.withInitialValue(Values.create("DB")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        )
    ),

    SOUND_EVENT(ASSET_NAMESPACE + ":soundEvent", "surround-sound", "5BBBD1",
        new AttributeDescriptorImpl("alarmEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Alarm event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Alarm event"))),
        new AttributeDescriptorImpl("aggressionEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Aggression event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Aggression event"))),
        new AttributeDescriptorImpl("breakingGlassEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Breaking glass event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("LBreaking glass event"))),
        new AttributeDescriptorImpl("gunshotEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Gunshot event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Gunshot event"))),
        new AttributeDescriptorImpl("intensityEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Intensity event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Intensity event"))),
        new AttributeDescriptorImpl("lastEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last event")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Last event")))
    ),

    ENVIRONMENT_SENSOR(ASSET_NAMESPACE + ":enviroment", "periodic-table-co2", "f18546",
        new AttributeDescriptorImpl("temperature", TEMPERATURE, Values.create(0),
            LABEL.withInitialValue(Values.create("Temperature")),
            DESCRIPTION.withInitialValue(Values.create("Temperature in celcius")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("nO2", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Nitrogen Level")),
            DESCRIPTION.withInitialValue(Values.create("µg/m3")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("relHumidity", PERCENTAGE, Values.create(0),
            LABEL.withInitialValue(Values.create("Humidity")),
            DESCRIPTION.withInitialValue(Values.create("Humidity in area")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("ozon", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Ozon Level")),
            DESCRIPTION.withInitialValue(Values.create("µg/m3")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("particlesPM1", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Particles PM 1")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("particlesPM2_5", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Particles PM 2.5")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("particlesPM10", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Particles PM 10")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        )
    ),

    LIGHT(ASSET_NAMESPACE + ":light", "lightbulb", "e6688a",
        new AttributeDescriptorImpl("lightStatus", BOOLEAN, Values.create(false),
            LABEL.withInitialValue(Values.create("Light status")),
            DESCRIPTION.withInitialValue(Values.create("Indicates if the light is on or off")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("lightDimLevel", PERCENTAGE, Values.create(0),
            LABEL.withInitialValue(Values.create("Light Dim Level")),
            DESCRIPTION.withInitialValue(Values.create("The level of dimming of the light")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("colorGBW", OBJECT, Values.createObject(),
            LABEL.withInitialValue(Values.create("Color RGBW")),
            DESCRIPTION.withInitialValue(Values.create("The RGBW color of the light")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("groupNumber", STRING, Values.create(""),
            LABEL.withInitialValue(Values.create("Group number")),
            DESCRIPTION.withInitialValue(Values.create("Which group this light belongs to")),
            READ_ONLY,
            RULE_STATE
        ),
        new AttributeDescriptorImpl("Scenario", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Scenario")),
            DESCRIPTION.withInitialValue(Values.create("The scenario the light are setup to")),
            READ_ONLY,
            RULE_STATE
        )
    ),

    LIGHT_CONTROLLER(ASSET_NAMESPACE + ":lightController", "light-switch", "9e5de2",
        new AttributeDescriptorImpl("lightAllStatus", BOOLEAN, Values.create(false),
            LABEL.withInitialValue(Values.create("Light all status")),
            DESCRIPTION.withInitialValue(Values.create("Indicates if all lights are on or off")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("lightAllDimLevel", PERCENTAGE, Values.create(0),
            LABEL.withInitialValue(Values.create("Light Dim Level")),
            DESCRIPTION.withInitialValue(Values.create("The level of dimming of all the lights")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("colorAllRGBW", OBJECT, Values.createObject(),
            LABEL.withInitialValue(Values.create("Color RGBW")),
            DESCRIPTION.withInitialValue(Values.create("The RGBW color of all the lights")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("Scenario", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("Scenario")),
            DESCRIPTION.withInitialValue(Values.create("The scenario the lights are setup to")),
            READ_ONLY,
            RULE_STATE
        )
    ),

    PEOPLE_COUNTER(ASSET_NAMESPACE + ":peopleCounter", "account-multiple", "4b5966", false,
        new AttributeDescriptorImpl("peopleCountIn", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count In")),
            DESCRIPTION.withInitialValue(Values.create("Cumulative number of people going into area")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("peopleCountOut", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count Out")),
            DESCRIPTION.withInitialValue(Values.create("Cumulative number of people leaving area")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("peopleCountInMinute", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count In Minute")),
            DESCRIPTION.withInitialValue(Values.create("Number of people going into area per minute")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("peopleCountOutMinute", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count Out Minute")),
            DESCRIPTION.withInitialValue(Values.create("Number of people leaving area per minute")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("peopleCountTotal", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count Total")),
            DESCRIPTION.withInitialValue(Values.create("cameraCountIn - cameraCountOut")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        ),
        new AttributeDescriptorImpl("peopleCountGrowth", NUMBER, Values.create(0),
            LABEL.withInitialValue(Values.create("People Count Growth")),
            DESCRIPTION.withInitialValue(Values.create("cameraCountIn - cameraCountOut")),
            READ_ONLY,
            RULE_STATE,
            STORE_DATA_POINTS
        )
    ),

    GATEWAY(ASSET_NAMESPACE + ":gateway", "router-wireless", null,
        new AttributeDescriptorImpl("clientId", STRING, null,
            LABEL.withInitialValue(Values.create("Client ID")),
            DESCRIPTION.withInitialValue(Values.create("Client ID for gateway to authorise")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("clientSecret", STRING, null,
            LABEL.withInitialValue(Values.create("Client Secret")),
            DESCRIPTION.withInitialValue(Values.create("Client secret for gateway to authorise")),
            READ_ONLY,
            SECRET
        ),
        new AttributeDescriptorImpl("status", CONNECTION_STATUS, null,
            LABEL.withInitialValue(Values.create("Connection status")),
            DESCRIPTION.withInitialValue(Values.create("Connection status of the gateway")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("disabled", BOOLEAN, Values.create(false))
    ),

    GROUP(ASSET_NAMESPACE + ":group", "folder", "B3B3B3",
        new AttributeDescriptorImpl("childAssetType", STRING, null, READ_ONLY)
    ),

    THING(ASSET_NAMESPACE + ":thing", "cube-outline", null);

    final protected String type;
    final protected String icon;
    final protected String color;
    final protected boolean accessPublicRead;
    final protected AttributeDescriptor[] attributeDescriptors;

    AssetType(String type, String icon, String color, AttributeDescriptor... attributeDescriptors) {
        this(type, icon, color, false, attributeDescriptors);
    }

    AssetType(String type, String icon, String color, boolean accessPublicRead, AttributeDescriptor... attributeDescriptors) {
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.accessPublicRead = accessPublicRead;
        this.attributeDescriptors = attributeDescriptors;
    }

    public static Optional<AssetType> getByValue(String value) {
        if (value == null)
            return Optional.empty();

        for (AssetType assetType : values()) {
            if (value.equals(assetType.getType()))
                return Optional.of(assetType);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public boolean getAccessPublicRead() {
        return accessPublicRead;
    }

    @Override
    public AttributeDescriptor[] getAttributeDescriptors() {
        return attributeDescriptors;
    }
}
