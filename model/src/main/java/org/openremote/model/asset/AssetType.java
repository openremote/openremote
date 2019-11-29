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
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.Constants.ASSET_NAMESPACE;
import static org.openremote.model.attribute.AttributeType.*;
import static org.openremote.model.attribute.AttributeValueType.SOUND_DB;
import static org.openremote.model.attribute.MetaItemType.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains the well-known URIs for functionality we want
 * to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType implements AssetDescriptor {

    BUILDING(ASSET_NAMESPACE + ":building", "office-building", null,
            SURFACE_AREA,
            GEO_STREET,
            GEO_CITY,
            GEO_COUNTRY,
            GEO_POSTAL_CODE),

    CITY(ASSET_NAMESPACE + ":city", "city", null,
        GEO_CITY,
        GEO_COUNTRY),

    AREA(ASSET_NAMESPACE + ":area", "home-city", null,
        GEO_CITY,
        GEO_COUNTRY,
        GEO_POSTAL_CODE),

    FLOOR(ASSET_NAMESPACE + ":floor", "stairs", null),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "home", null),

    ROOM(ASSET_NAMESPACE + ":room", "door", null),

    AGENT(ASSET_NAMESPACE + ":agent", "cogs", null),

    CONSOLE(ASSET_NAMESPACE + ":console", "monitor-cellphone", null),

    MICROPHONE(ASSET_NAMESPACE + ":microphone", "microphone", "5bbbd1",
        new AttributeDescriptorImpl("soundLevel", SOUND_DB, Values.create(0),
            LABEL.withInitialValue(Values.create("Sound level")),
            READ_ONLY,
            DESCRIPTION.withInitialValue(Values.create("Current sound level")))),

    ENVIRONMENT_SENSOR(ASSET_NAMESPACE + ":enviroment", "periodic-table-co2", null),

    LIGHT(ASSET_NAMESPACE + ":light", "lightbulb", null),

    CAMERA(ASSET_NAMESPACE + ":camera", "camera", null),

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
