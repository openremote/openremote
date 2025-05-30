/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.manager;

import java.util.Map;

/**
 * Implements MapLibre style spec
 *
 * @see https://maplibre.org/maplibre-style-spec/root/
 */
public class MapConfig {
    /**
     * Custom OpenRemote property to override mapconfig with external mapconfig
     * Expects a style.json URL
     */
    public String override;
    /**
     * Custom OpenRemote property to configure mapsettings per realm
     */
    public Map<String, MapRealmConfig> options;
    public Map<String, MapSourceConfig> sources;
    public String glyphs;
    public String sprite;
    public Object[] layers;
}
