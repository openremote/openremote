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
package org.openremote.model.asset.macro;

import elemental.json.Json;
import org.openremote.model.AttributeType;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAttributeWrapper;
import org.openremote.model.asset.AttributeWrapperFilter;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.util.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

public class MacroConfiguration extends AbstractAttributeWrapper<MacroConfiguration> {
    public static class Filter implements AttributeWrapperFilter<MacroConfiguration> {
        @Override
        public List<MacroConfiguration> getAllWrapped(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            return getAll(assetAttributes, excludeInvalid)
                .stream()
                .map(assetAttribute -> new MacroConfiguration(assetAttribute, false))
                .collect(Collectors.toList());
        }

        @Override
        public List<AssetAttribute> getAll(List<AssetAttribute> assetAttributes, boolean excludeInvalid) {
            // Macro Attribute wrappers should have macro protocol name as the attribute value
            return assetAttributes
                .stream()
                .filter(attribute ->
                    MacroConfiguration.valueIsProtocolName(attribute) && (!excludeInvalid || MacroConfiguration.isValid(attribute))
                )
                .collect(Collectors.toList());
        }
    }

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":macro";
    protected static final AttributeWrapperFilter<MacroConfiguration> filter = new Filter();

    public MacroConfiguration(String attributeName) {
        this(new AssetAttribute(attributeName, AttributeType.STRING, Json.create(PROTOCOL_NAME)));
    }

    public MacroConfiguration(AssetAttribute attribute) {
        super(attribute);
    }

    public MacroConfiguration(AssetAttribute attribute, boolean initialise) {
        super(attribute, initialise);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isValid(getAttribute());
    }

    @Override
    public void initialise() {
        // Set value as protocol name
        getAttribute().setValue(Json.create(PROTOCOL_NAME));
    }

    @Override
    public AttributeWrapperFilter<MacroConfiguration> getFilter() {
        return filter;
    }

    public List<MacroAction> getActions() {
        return getAttribute().getMeta().all()
            .stream()
            .filter(metaItem -> metaItem.getName().equals(AssetMeta.MACRO_ACTION.getName()))
            .map(metaItem -> new MacroAction(metaItem.getValueAsObject()))
            .collect(Collectors.toList());
    }

    public void setActions(List<MacroAction> actions) {
        Meta meta = getAttribute().getMeta() != null ? getAttribute().getMeta() : new Meta();
        meta = meta.removeAll(AssetMeta.MACRO_ACTION);
        Meta finalMeta = meta;
        actions.forEach(action -> finalMeta.add(action.asMetaItem()));
    }

    public void addAction(MacroAction action) {
        Meta meta = getAttribute().getMeta() != null ? getAttribute().getMeta() : new Meta();
        meta.add(action.asMetaItem());
        getAttribute().setMeta(meta);
    }

    public void removeAction(MacroAction action) {
        if (!getAttribute().hasMeta()) {
            return;
        }

        List<MetaItem> metaItems = getAttribute().getMeta().all();
        IntStream.range(0, metaItems.size())
            .filter(i -> metaItems.get(i).getName().equals(AssetMeta.MACRO_ACTION.getName()))
            .filter(i -> JsonUtil.equals(metaItems.get(i).getValueAsObject(), action.asJsonValue()))
            .forEach(i -> getAttribute().getMeta().remove(i));
    }

    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    public static boolean isValid(AssetAttribute assetAttribute) {
        MetaItem item = assetAttribute.firstMetaItem(AssetMeta.MACRO_ACTION);
        return AbstractAttributeWrapper.isValid(assetAttribute) && valueIsProtocolName(assetAttribute) && item != null;
    }

    public static boolean valueIsProtocolName(AssetAttribute assetAttribute) {
        // Value must be protocol name and must be at least one macro action
        String value = assetAttribute.getValueAsString();
        return value != null && value.equals(PROTOCOL_NAME);
    }
}
