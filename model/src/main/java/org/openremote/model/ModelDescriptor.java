/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model;

import org.openremote.model.asset.Asset;

import java.lang.annotation.*;

/**
 * To be used on {@link Asset} classes to include another class in descriptor scans (only used when {@link
 * AssetModelProvider#useAutoScan} is true), otherwise the descriptors must be explicitly specified in the appropriate
 * {@link AssetModelProvider} descriptor getter methods.
 */
@SuppressWarnings("rawtypes")
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ModelDescriptors.class)
public @interface ModelDescriptor {

    /**
     * The {@link Asset} type to which discovered descriptors should be associated (can be abstract or concrete).
     */
    Class<? extends Asset> assetType();

    /**
     * Class that should be scanned for public static fields for descriptors.
     */
    Class<?> provider();
}
