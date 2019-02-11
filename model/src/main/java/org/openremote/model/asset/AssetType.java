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

import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.stream.Stream;

import static org.openremote.model.Constants.ASSET_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.attribute.AttributeValueType.NUMBER;
import static org.openremote.model.attribute.AttributeValueType.STRING;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains the well-known URIs for functionality we want
 * to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType implements AssetTypeDescriptor {

    CUSTOM(null, "cube"),

    BUILDING(ASSET_NAMESPACE + ":building", "building",
             new AssetAttribute("area", NUMBER)
                 .setMeta(
                     new MetaItem(LABEL, Values.create("Surface Area")),
                     new MetaItem(DESCRIPTION, Values.create("Floor area of building measured in m²")),
                     new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/area"))
                 ),
             new AssetAttribute("geoStreet", STRING)
                 .setMeta(
                     new MetaItem(LABEL, Values.create("Street")),
                     new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoStreet"))
                 ),
             new AssetAttribute("geoPostalCode", NUMBER)
                 .setMeta(
                     new MetaItem(LABEL, Values.create("Postal Code")),
                     new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoPostalCode"))
                 ),
             new AssetAttribute("geoCity", STRING)
                 .setMeta(
                     new MetaItem(LABEL, Values.create("City")),
                     new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCity"))
                 ),
             new AssetAttribute("geoCountry", STRING)
                 .setMeta(
                     new MetaItem(LABEL, Values.create("Country")),
                     new MetaItem(ABOUT, Values.create("http://project-haystack.org/tag/geoCountry"))
                 )
    ),

    FLOOR(ASSET_NAMESPACE + ":floor", "server"),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "cubes"),

    ROOM(ASSET_NAMESPACE + ":room", "cube"),

    AGENT(ASSET_NAMESPACE + ":agent", "cogs"),

    CONSOLE(ASSET_NAMESPACE + ":console", "mobile-alt"),

    THING(ASSET_NAMESPACE + ":thing", "cog");

    final protected String value;
    final protected String icon;
    final protected boolean accessPublicRead;
    final protected AssetAttribute[] defaultAttributes;

    AssetType(String value, String icon, AssetAttribute... defaultAttributes) {
        this(value, icon, false, defaultAttributes);
    }

    AssetType(String value, String icon, boolean accessPublicRead, AssetAttribute... defaultAttributes) {
        this.value = value;
        this.icon = icon;
        this.accessPublicRead = accessPublicRead;
        this.defaultAttributes = defaultAttributes;
    }

    public static AssetTypeDescriptor[] valuesSorted() {
        List<AssetTypeDescriptor> list = new ArrayList<>(Arrays.asList(values()));

        list.sort(Comparator.comparing(AssetTypeDescriptor::getName));
        if (list.contains(CUSTOM)) {
            // CUSTOM should be first
            list.remove(CUSTOM);
            list.add(0, CUSTOM);
        }

        return list.toArray(new AssetTypeDescriptor[list.size()]);
    }

    public static Optional<AssetType> getByValue(String value) {
        if (value == null)
            return Optional.empty();

        for (AssetType assetType : values()) {
            if (value.equals(assetType.getValue()))
                return Optional.of(assetType);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getValue() {
        return value;
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
    public Stream<AssetAttribute> getDefaultAttributes() {
        return defaultAttributes != null ? Arrays.stream(defaultAttributes) : Stream.empty();
    }
}
