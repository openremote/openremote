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
package org.openremote.agent.protocol.macro;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.MetaItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.agent.protocol.macro.MacroProtocol.META_MACRO_ACTION;
import static org.openremote.agent.protocol.macro.MacroProtocol.PROTOCOL_NAME;
import static org.openremote.model.asset.agent.ProtocolConfiguration.getProtocolName;
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;

/**
 * Agent attributes can be macro configurations.
 * <p>
 * A macro configuration attribute has {@link MacroProtocol#META_MACRO_ACTION} items attached to it, each
 * item is a sequence of asset state changes to be executed.
 */
final public class MacroConfiguration {

    private MacroConfiguration() {
    }

    public static AssetAttribute initMacroConfiguration(AssetAttribute attribute) {
        return initProtocolConfiguration(attribute, MacroProtocol.PROTOCOL_NAME);
    }

    public static boolean isMacroConfiguration(AssetAttribute attribute) {
        return getProtocolName(attribute)
            .map(MacroProtocol.PROTOCOL_NAME::equals)
            .orElse(false);
    }

    public static boolean validateMacroConfiguration(AssetAttribute attribute, AttributeValidationResult result) {
        boolean failure = false;

        if (!isMacroConfiguration(attribute)) {
            failure = true;
            if (result != null) {
                result.addAttributeFailure(new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_MISMATCH, PROTOCOL_NAME));
            }
        }

        int actionCount = 0;
        if (attribute.getMeta() != null && !attribute.getMeta().isEmpty()) {
            for (int i = 0; i < attribute.getMeta().size(); i++) {
                MetaItem metaItem = attribute.getMeta().get(i);
                if (isMetaNameEqualTo(metaItem, META_MACRO_ACTION)) {
                    actionCount++;
                    if (!MacroAction.fromValue(metaItem.getValue().orElse(null)).isPresent()) {
                        failure = true;
                        if (result == null) {
                            break;
                        }
                        result.addMetaFailure(
                            i, new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_VALUE_MISMATCH, "Macro Action")
                        );
                    }
                }
            }
        }

        if (actionCount == 0) {
            failure = true;
            if (result != null) {
                result.addMetaFailure(
                    new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_MISSING, META_MACRO_ACTION)
                );
            }
        }

        return !failure;
    }

    public static boolean isValidMacroConfiguration(AssetAttribute attribute) {

        return validateMacroConfiguration(attribute, null);
    }

    public static Stream<MacroAction> getMacroActionsStream(AssetAttribute attribute) {
        return attribute == null ? Stream.empty() :
            attribute
                .getMetaStream()
                .filter(isMetaNameEqualTo(META_MACRO_ACTION))
                .map(metaItem -> metaItem.getValue().orElse(null))
                .map(MacroAction::fromValue)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static List<MacroAction> getMacroActions(AssetAttribute attribute) {
        return getMacroActionsStream(attribute)
            .collect(Collectors.toList());
    }

    public static AssetAttribute setMacroActions(AssetAttribute attribute, MacroAction... actions) {
        return setMacroActions(attribute, Arrays.stream(actions));
    }

    public static UnaryOperator<AssetAttribute> setMacroActions(MacroAction... actions) {
        return attribute -> setMacroActions(attribute, actions);
    }

    public static AssetAttribute setMacroActions(AssetAttribute attribute, Collection<MacroAction> actions) {
        return setMacroActions(attribute, actions.stream());
    }

    public static UnaryOperator<AssetAttribute> setMacroActions(Collection<MacroAction> actions) {
        return attribute -> setMacroActions(attribute, actions);
    }

    public static AssetAttribute setMacroActions(AssetAttribute attribute, Stream<MacroAction> actions) {
        if (attribute == null)
            return null;

        attribute.getMeta().addAll(
            actions
                .map(MacroAction::toMetaItem)
                .collect(Collectors.toList())
        );

        return attribute;
    }

    public static Optional<Integer> getMacroActionIndex(AssetAttribute attribute) {
        return attribute == null ? Optional.empty()
            : attribute.getMetaItem(MacroProtocol.META_MACRO_ACTION_INDEX)
                .flatMap(AbstractValueHolder::getValueAsInteger);
    }
}
