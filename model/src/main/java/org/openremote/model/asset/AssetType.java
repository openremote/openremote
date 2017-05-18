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

import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.stream.Stream;

import static org.openremote.model.Constants.ASSET_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.attribute.AttributeType.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType {

    CUSTOM(null, "cube", null),

    BUILDING(ASSET_NAMESPACE + ":building", "building", Arrays.asList(
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
            ))
    ),

    FLOOR(ASSET_NAMESPACE + ":floor", "server", null),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "cubes", null),

    ROOM(ASSET_NAMESPACE + ":room", "cube", null),

    FLIGHT(ASSET_NAMESPACE + ":flight", "plane", Arrays.asList(
        new AssetAttribute("code", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Flight Code"))
            ),
        new AssetAttribute("airline", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Airline"))
            ),
        new AssetAttribute("planeRegistration", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Plane Registration"))
            ),
        new AssetAttribute("planeType", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Plane Type"))
            ),
        new AssetAttribute("passengerCapacity", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Passenger Capacity"))
            ),
        new AssetAttribute("originAirport", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Origin Airport"))
            ),
        new AssetAttribute("originCountry", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Origin Country"))
            ),
        new AssetAttribute("originRegion", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Origin Region"))
            ),
        new AssetAttribute("destinationAirport", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Destination Airport"))
            ),
        new AssetAttribute("destinationCountry", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Destination Country"))
            ),
        new AssetAttribute("destinationRegion", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Destination Region"))
            ),
        new AssetAttribute("departureDateTime", DATETIME)
            .setMeta(
                new MetaItem(LABEL, Values.create("Departure Time"))
            ),
        new AssetAttribute("departureGate", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Departure Gate"))
            ),
        new AssetAttribute("departurePier", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Departure Pier"))
            ),
        new AssetAttribute("arrivalDateTime", DATETIME)
            .setMeta(
                new MetaItem(LABEL, Values.create("Arrival Time"))
            ),
        new AssetAttribute("arrivalGate", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Arrival Gate"))
            ),
        new AssetAttribute("arrivalPier", STRING)
            .setMeta(
                new MetaItem(LABEL, Values.create("Arrival Pier"))
            ),
        new AssetAttribute("priority", BOOLEAN)
            .setMeta(
                new MetaItem(LABEL, Values.create("Priority"))
            ))
    ),

    AGENT(ASSET_NAMESPACE + ":agent", "gears", null),

    THING(ASSET_NAMESPACE + ":thing", "gear", null);

    final protected String value;
    final protected String icon;
    final protected List<AssetAttribute> defaultAttributes;

    AssetType(String value, String icon, List<AssetAttribute> defaultAttributes) {
        this.value = value;
        this.icon = icon;
        this.defaultAttributes = defaultAttributes;
    }

    public String getValue() {
        return value;
    }

    public String getIcon() {
        return icon;
    }

    public Stream<AssetAttribute> getDefaultAttributes() {
        return defaultAttributes != null ? defaultAttributes.stream() : Stream.empty();
    }

    public static AssetType[] valuesSorted() {
        List<AssetType> list = new ArrayList<>(Arrays.asList(values()));

        list.sort(Comparator.comparing(Enum::name));
        if (list.contains(CUSTOM)) {
            // CUSTOM should be first
            list.remove(CUSTOM);
            list.add(0, CUSTOM);
        }

        return list.toArray(new AssetType[list.size()]);
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
}
