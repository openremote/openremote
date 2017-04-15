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
package org.openremote.agent3.protocol.macro;

import elemental.json.Json;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.util.JsonUtil;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.openremote.model.Attribute.isAttributeType;
import static org.openremote.model.Attribute.isAttributeValid;
import static org.openremote.model.AttributeType.STRING;
import static org.openremote.model.asset.AssetAttribute.hasAssetMetaItem;
import static org.openremote.model.asset.AssetAttribute.isAssetMetaItem;

/**
 * Agent attributes can be macro configurations.
 * <p>
 * A macro configuration attribute has {@link MacroAction#MACRO_ACTION} items attached to it, each
 * item is a sequence of asset state changes to be executed.
 */
final public class MacroConfiguration {

    private MacroConfiguration() {
    }

    public static Function<AssetAttribute, AssetAttribute> initMacroConfiguration() {
        return attribute -> {
            attribute.setType(AttributeType.STRING);
            attribute.setValue(Json.create(MacroProtocol.MACRO_PROTOCOL_NAME));
            return attribute;
        };
    }

    public static Predicate<Attribute> isMacroConfiguration() {
        return attribute -> {
            String value = attribute.getValueAsString();
            return value != null && value.equals(MacroProtocol.MACRO_PROTOCOL_NAME);
        };
    }

    public static Predicate<Attribute> isValidMacroConfiguration() {
        return isAttributeValid()
            .and(isAttributeType(STRING))
            .and(isMacroConfiguration())
            .and(hasAssetMetaItem(MacroAction.MACRO_ACTION)); // Must have at least one macro action
    }

    public static Function<AssetAttribute, Stream<MacroAction>> getMacroActions() {
        return attribute -> attribute.getMetaItemStream()
            .filter(isAssetMetaItem(MacroAction.MACRO_ACTION))
            .map(MacroAction.getMacroActionFromMetaItem());
    }

    public static Function<AssetAttribute, AssetAttribute> addMacroAction(MacroAction action) {
        return attribute -> attribute.setMeta(
            attribute.getMeta().add(MacroAction.getMetaItemFromMacroAction().apply(action))
        );
    }

    public static Function<AssetAttribute, AssetAttribute> removeMacroAction(MacroAction action) {
        return attribute -> {
            if (!attribute.hasMeta()) {
                return attribute;
            }
            List<MetaItem> metaItems = attribute.getMeta().all();
            IntStream.range(0, metaItems.size())
                .filter(i -> isAssetMetaItem(MacroAction.MACRO_ACTION).test(metaItems.get(i)))
                .filter(i -> JsonUtil.equals(metaItems.get(i).getValueAsObject(), action.asJsonValue()))
                .forEach(i -> attribute.getMeta().remove(i));

            return attribute;
        };
    }
}
