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

import java.util.*;

import static org.openremote.model.Constants.ASSET_NAMESPACE;
import static org.openremote.model.attribute.AttributeType.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains the well-known URIs for functionality we want
 * to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType implements AssetDescriptor {

    CUSTOM(null, "cube"),

    BUILDING(ASSET_NAMESPACE + ":building", "building",
            SURFACE_AREA,
            GEO_STREET,
            GEO_CITY,
            GEO_COUNTRY,
            GEO_POSTAL_CODE),

    FLOOR(ASSET_NAMESPACE + ":floor", "server"),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "cubes"),

    ROOM(ASSET_NAMESPACE + ":room", "cube"),

    AGENT(ASSET_NAMESPACE + ":agent", "cogs"),

    CONSOLE(ASSET_NAMESPACE + ":console", "mobile-alt"),

    THING(ASSET_NAMESPACE + ":thing", "cog");

    final protected String type;
    final protected String icon;
    final protected boolean accessPublicRead;
    final protected AttributeDescriptor[] attributeDescriptors;

    AssetType(String type, String icon, AttributeDescriptor... attributeDescriptors) {
        this(type, icon, false, attributeDescriptors);
    }

    AssetType(String type, String icon, boolean accessPublicRead, AttributeDescriptor... attributeDescriptors) {
        this.type = type;
        this.icon = icon;
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
    public boolean getAccessPublicRead() {
        return accessPublicRead;
    }

    @Override
    public Optional<AttributeDescriptor[]> getAttributeDescriptors() {
        return Optional.ofNullable(attributeDescriptors);
    }
}
