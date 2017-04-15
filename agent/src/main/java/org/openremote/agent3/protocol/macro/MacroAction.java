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
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.AttributeState;
import org.openremote.model.MetaItem;

import java.util.function.Function;

import static org.openremote.model.Constants.ASSET_META_NAMESPACE;

/**
 * A desired {@link AttributeState} and a delay in milli seconds before that state
 * is applied when the macro executes.
 */
public class MacroAction {

    public static final String MACRO_ACTION = ASSET_META_NAMESPACE + ":macroAction";
    protected AttributeState attributeState;
    protected int delayMilliseconds;

    static Function<MetaItem, MacroAction> getMacroActionFromMetaItem() {
        return metaItem -> new MacroAction(metaItem.getValueAsObject());
    }

    static Function<MacroAction, MetaItem> getMetaItemFromMacroAction() {
        return macroAction -> new MetaItem(MACRO_ACTION, macroAction.asJsonValue());
    }

    public MacroAction(JsonObject jsonObject) {
        attributeState = new AttributeState(jsonObject.getObject("attributeState"));
        delayMilliseconds = new Double(jsonObject.getNumber("delay")).intValue();
    }

    public MacroAction(AttributeState attributeState, int delayMilliseconds) {
        this.attributeState = attributeState;
        this.delayMilliseconds = delayMilliseconds;
    }

    public AttributeState getAttributeState() {
        return attributeState;
    }

    public void setAttributeState(AttributeState attributeState) {
        this.attributeState = attributeState;
    }

    public int getDelayMilliseconds() {
        return delayMilliseconds;
    }

    public void setDelayMilliseconds(int delayMilliseconds) {
        this.delayMilliseconds = delayMilliseconds;
    }

    public JsonValue asJsonValue() {
        JsonObject jsonObect = Json.createObject();
        jsonObect.put("attributeState", attributeState != null ? attributeState.asJsonValue() : Json.create(null));
        jsonObect.put("delay", Json.create(delayMilliseconds));
        return jsonObect;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeState='" + attributeState + '\'' +
            ", delay='" + delayMilliseconds + '\'' +
            '}';
    }
}
