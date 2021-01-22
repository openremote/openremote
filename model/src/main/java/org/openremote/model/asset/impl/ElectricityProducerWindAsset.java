/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.asset.impl;

import org.openremote.model.asset.AssetDescriptor;

import javax.persistence.Entity;

@Entity
public class ElectricityProducerWindAsset extends ElectricityProducerAsset {

    public static final AssetDescriptor<ElectricityProducerWindAsset> DESCRIPTOR = new AssetDescriptor<>("wind-turbine", "4B87EA", ElectricityProducerWindAsset.class);

    ElectricityProducerWindAsset() {
    }

    public ElectricityProducerWindAsset(String name) {
        super(name);
    }
}
