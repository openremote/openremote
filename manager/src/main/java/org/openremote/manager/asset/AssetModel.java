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
package org.openremote.manager.asset;

import org.openremote.model.asset.AssetModelProvider;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.openremote.model.asset.AssetType.CUSTOM;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * Explore the asset model of this project and access the {@link AssetModelProvider}s through {@link ServiceLoader}.
 */
public class AssetModel {

    private static final Logger LOG = Logger.getLogger(AssetModel.class.getName());

    public final static MetaItemDescriptor[] META_ITEM_DESCRIPTORS;
    public final static String META_ITEM_RESTRICTED_READ_SQL_FRAGMENT;
    public final static String META_ITEM_PUBLIC_READ_SQL_FRAGMENT;

    public final static AssetDescriptor[] WELL_KNOWN_ASSET_TYPES;

    public final static AttributeDescriptor[] WELL_KNOWN_ATTRIBUTE_TYPES;

    static {
        List<MetaItemDescriptor> metaItemDescriptorList = new ArrayList<>();
        ServiceLoader.load(AssetModelProvider.class).forEach(assetModelProvider -> {
            LOG.fine("Adding meta item descriptors of: " + assetModelProvider);
            metaItemDescriptorList.addAll(Arrays.asList(assetModelProvider.getMetaItemDescriptors()));
        });
        META_ITEM_DESCRIPTORS = metaItemDescriptorList.toArray(new MetaItemDescriptor[metaItemDescriptorList.size()]);

        META_ITEM_RESTRICTED_READ_SQL_FRAGMENT =
            " ('" + streamMetaItemDescriptors().filter(i -> i.getAccess().restrictedRead).map(MetaItemDescriptor::getUrn).collect(joining("','")) + "')";
        META_ITEM_PUBLIC_READ_SQL_FRAGMENT =
            " ('" + streamMetaItemDescriptors().filter(i -> i.getAccess().publicRead).map(MetaItemDescriptor::getUrn).collect(joining("','")) + "')";

        List<AssetDescriptor> assetTypeList = new ArrayList<>();
        ServiceLoader.load(AssetModelProvider.class).forEach(assetModelProvider -> {
            LOG.fine("Adding asset type descriptors of: " + assetModelProvider);
            assetTypeList.addAll(Arrays.asList(assetModelProvider.getAssetDescriptors()));
        });

        WELL_KNOWN_ASSET_TYPES = assetTypeList.toArray(new AssetDescriptor[assetTypeList.size()]);

        List<AttributeDescriptor> attributeList = new ArrayList<>();
        ServiceLoader.load(AssetModelProvider.class).forEach(assetModelProvider -> {
            LOG.fine("Adding asset type descriptors of: " + assetModelProvider);
            attributeList.addAll(Arrays.asList(assetModelProvider.getAttributeDescriptors()));
        });

        WELL_KNOWN_ATTRIBUTE_TYPES = attributeList.toArray(new AttributeDescriptor[attributeList.size()]);
    }

    public static Stream<MetaItemDescriptor> streamMetaItemDescriptors() {
        return Arrays.stream(META_ITEM_DESCRIPTORS);
    }

    public static Optional<MetaItemDescriptor> getMetaItemDescriptor(String urn) {
        if (isNullOrEmpty(urn))
            return Optional.empty();
        for (MetaItemDescriptor metaItemDescriptor : META_ITEM_DESCRIPTORS) {
            if (metaItemDescriptor.getUrn().equals(urn))
                return Optional.of(metaItemDescriptor);
        }
        return Optional.empty();
    }

    public static boolean isMetaItemRestrictedRead(MetaItem metaItem) {
        return getMetaItemDescriptor(metaItem.getName().orElse(null))
            .map(meta -> meta.getAccess().restrictedRead)
            .orElse(false);
    }

    public static boolean isMetaItemRestrictedWrite(MetaItem metaItem) {
        return getMetaItemDescriptor(metaItem.getName().orElse(null))
            .map(meta -> meta.getAccess().restrictedWrite)
            .orElse(false);
    }

    public static boolean isMetaItemPublicRead(MetaItem metaItem) {
        return getMetaItemDescriptor(metaItem.getName().orElse(null))
            .map(meta -> meta.getAccess().publicRead)
            .orElse(false);
    }

    public static AssetDescriptor[] getAssetTypesSorted() {
        List<AssetDescriptor> list = new ArrayList<>(Arrays.asList(WELL_KNOWN_ASSET_TYPES));

        list.sort(Comparator.comparing(AssetDescriptor::getName));
        if (list.contains(CUSTOM)) {
            // CUSTOM should be first
            list.remove(CUSTOM);
            list.add(0, CUSTOM);
        }

        return list.toArray(new AssetDescriptor[list.size()]);
    }

    public static Optional<AssetDescriptor> getAssetDescriptor(String urn) {
        if (urn == null)
            return Optional.empty();

        for (AssetDescriptor assetType : WELL_KNOWN_ASSET_TYPES) {
            if (urn.equals(assetType.getType()))
                return Optional.of(assetType);
        }
        return Optional.empty();
    }

    public static Optional<AttributeDescriptor> getAttributeDescriptor(String name) {
        if (name == null)
            return Optional.empty();

        for (AttributeDescriptor attributeDescriptor : WELL_KNOWN_ATTRIBUTE_TYPES) {
            if (name.equals(attributeDescriptor.getName()))
                return Optional.of(attributeDescriptor);
        }
        return Optional.empty();
    }
}
