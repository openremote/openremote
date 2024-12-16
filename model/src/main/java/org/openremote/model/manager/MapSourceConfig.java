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

/**
 * Implements MapLibre style spec sources property.
 * 
 * @see https://maplibre.org/maplibre-style-spec/sources
 */
public class MapSourceConfig {
	/**
	 * The type of the source.
	 * 
	 * @implSpec Required enum. Possible values: <code>vector</code>.
	 */
	protected String type;
	/**
	 * A URL to a TileJSON resource.
	 * 
	 * @implSpec Optional string. Supported protocols are <code>http:</code> and <code>https:</code>.
	 */
	protected String url;
	/**
	 * An array of one or more tile source URLs, as in the TileJSON spec.
	 * 
	 * @implSpec Optional array. Possible values: vector.
	 */
	protected String[] tiles;
	/**
	 * An array containing the longitude and latitude of the southwest and northeast corners of the source's bounding box in the following order: <code>[sw.lng, sw.lat, ne.lng, ne.lat]</code>. When this property is included in a source, no tiles outside of the given bounds are requested by MapLibre.
	 * 
	 * @implSpec Optional array. Possible values: vector.
	 */
	protected float[] bounds;
	/**
	 * Influences the y direction of the tile coordinates. The global-mercator (aka Spherical Mercator) profile is assumed.
   * <p><ul>
	 * <li><code>xyz</code>: Slippy map tilenames scheme.</li>
   * <li><code>tms</code>: OSGeo spec scheme.</li>
	 * </ul></p>
	 * 
	 * @implSpec Optional enum. Possible values: <code>xyz</code>, <code>tms</code>. Defaults to <code>xyz</code>.
	 */
	protected String scheme;
	/**
   * Minimum zoom level for which tiles are available, as in the TileJSON spec.
	 * 
	 * @implSpec Optional number. Defaults to <code>0</code>.
	 */
	protected Integer minzoom;
	/**
	 * Maximum zoom level for which tiles are available, as in the TileJSON spec. Data from tiles at the maxzoom are used when displaying the map at higher zoom levels.
	 * 
	 * @implSpec Optional number. Defaults to <code>22</code>.
	 */
	protected Integer maxzoom;
	/**
	 * Contains an attribution to be displayed when the map is shown to a user.
	 * 
	 * @implSpec Optional string.
	 */
	protected String attribution;
	// protected String promoteId;
	// protected boolean volatile;
}
