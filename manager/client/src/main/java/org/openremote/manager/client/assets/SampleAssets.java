/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.assets;

import org.geojson.Point;
import org.openremote.manager.client.assets.asset.Asset;

import java.util.*;

/**
 * TODO Remove this test class
 */
public class SampleAssets {

    public static String getMapFeaturesJson(String id, String title, Point location) {
        return "{" +
            "\"type\":\"FeatureCollection\",\"features\":" +
            "[" +
            "{" +
            "\"type\":\"Feature\"," +
            "\"properties\":" +
            "{" +
            "\"id\":\"" + id + "\"," +
            "\"title\":\"" + title + "\"}," +
            "\"geometry\":" +
            "{" +
            "\"type\":\"Point\"," +
            "\"coordinates\":" +
            "[" +
            location.getCoordinates().getLongitude() + ", " +
            location.getCoordinates().getLatitude() +
            "]" +
            "}" +
            "}" +
            "]" +
            "}";
    }

    public static List<Asset> queryAll(Asset parent) {
        if (Asset.isRoot(parent)) {
            return ROOT_CHILDREN;
        } else if (parent.getType().equals(Asset.Type.COMPOSITE.name())) {
            if (parent.getId().equals("composite:gateways")) {
                return GATEWAYS;
            }

            if (parent.getId().equals("1000")) {
                return SENSORS;
            }
        }
        return new ArrayList<>();
    }

    public static final Asset ROOT = new Asset(
        Asset.ROOT_ID,
        Asset.ROOT_TYPE,
        Asset.ROOT_LABEL,
        getMapFeaturesJson(Asset.ROOT_ID, Asset.ROOT_LABEL, Asset.ROOT_LOCATION)
    );

    public static final List<Asset> ROOT_CHILDREN = new ArrayList<Asset>() {{
        addAll(Arrays.asList(
            new Asset(
                "composite:gateways",
                Asset.Type.COMPOSITE.name(),
                "Gateways",
                getMapFeaturesJson("composite:gateways", "Gateways", new Point(5.460315214821094, 51.44541688237109, 0))
            ),
            new Asset(
                "composite:buildings",
                Asset.Type.COMPOSITE.name(),
                "Buildings",
                getMapFeaturesJson("composite:buildings", "Buildings", new Point(5.479108567142475, 51.43873552530604, 0))
            ),
            new Asset(
                "composite:rooms",
                Asset.Type.COMPOSITE.name(),
                "Rooms",
                getMapFeaturesJson("composite:rooms", "Rooms", new Point(5.454904312991857, 51.42830131622847, 0))
            ),
            new Asset(
                "composite:thermostats",
                Asset.Type.COMPOSITE.name(),
                "Thermostats",
                getMapFeaturesJson("composite:thermostats", "Thermostats", new Point(5.4866616677283275, 51.432154224794346, 0))
            )
        ));
    }};

    public static final List<Asset> GATEWAYS = new ArrayList<Asset>() {{

        for (int i = 1000; i < 1100; i++) {
            add(
                new Asset(
                    Integer.toString(i),
                    Asset.Type.COMPOSITE.name(),
                    "Gateway " + i,
                    getMapFeaturesJson(Integer.toString(i), "Gateway" + i, new Point(5.460315214821094, 51.44541688237109, 0))
                )
            );
        }
    }};

    public static final List<Asset> SENSORS = Arrays.asList(
        new Asset(
            "11",
            Asset.Type.SENSOR.name(),
            "Sensor 1",
            getMapFeaturesJson("11", "Sensor 1", new Point(5.460315214821094, 51.44541688237109, 0))
        ),
        new Asset(
            "22",
            Asset.Type.SENSOR.name(),
            "Sensor 2",
            getMapFeaturesJson("22", "Sensor 2", new Point(5.460315214821094, 51.44541688237109, 0))
        ),
        new Asset(
            "33",
            Asset.Type.SENSOR.name(),
            "Sensor 3",
            getMapFeaturesJson("33", "Sensor 3", new Point(5.460315214821094, 51.44541688237109, 0))
        )
    );

    public static List<String> getSelectedAssetPath(String assetId) {
        List<String> path = new ArrayList<>();
        // TODO: We must build the asset path here, by finding the asset first, then its parents recursively
        switch (assetId) {
            case "composite:gateways":
                path.add("composite:gateways");
                break;
            case "composite:buildings":
                path.add("composite:buildings");
                break;
            case "composite:rooms":
                path.add("composite:rooms");
                break;
            case "composite:thermostats":
                path.add("composite:thermostats");
                break;
            case "11":
                path.add("composite:gateways");
                path.add("1000");
                path.add("11");
                break;
            case "22":
                path.add("composite:gateways");
                path.add("1000");
                path.add("22");
                break;
            case "33":
                path.add("composite:gateways");
                path.add("1000");
                path.add("33");
                break;
            default:
                path.add("composite:gateways");
                path.add(assetId);
                break;
        }
        return path;
    }

    public static final Map<String, Asset> ALL_BY_ID = new LinkedHashMap<String, Asset>() {{
        for (Asset asset : ROOT_CHILDREN) {
            put(asset.getId(), asset);
        }
        for (Asset asset : GATEWAYS) {
            put(asset.getId(), asset);
        }
        for (Asset asset : SENSORS) {
            put(asset.getId(), asset);
        }

    }};
}
