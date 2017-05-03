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
import org.openremote.model.util.JsonUtil;

import java.util.Objects;
import java.util.Optional;

import static org.openremote.agent3.protocol.macro.MacroConfiguration.META_MACRO_ACTION;

/**
 * A desired {@link AttributeState} and a delay in milli seconds before that state
 * is applied when the macro executes.
 */
public class MacroAction {

    protected AttributeState attributeState;
    protected int delayMilliseconds;

    public MacroAction(AttributeState attributeState) {
        Objects.requireNonNull(attributeState);
        this.attributeState = attributeState;
    }

    public MacroAction(AttributeState attributeState, int delayMilliseconds) {
        this(attributeState);
        setDelayMilliseconds(delayMilliseconds);
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
        this.delayMilliseconds = Math.max(0, delayMilliseconds);
    }

    public JsonValue toJsonValue() {
        JsonObject jsonObect = Json.createObject();
        jsonObect.put("attributeState", attributeState != null ? attributeState.toJsonValue() : Json.create(null));
        jsonObect.put("delay", Json.create(delayMilliseconds));
        return jsonObect;
    }

    public MetaItem toMetaItem() {
        return new MetaItem(META_MACRO_ACTION, toJsonValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeState='" + attributeState + '\'' +
            ", delay='" + delayMilliseconds + '\'' +
            '}';
    }

    public static Optional<MacroAction> fromJsonValue(JsonValue jsonValue) {
        return JsonUtil.asJsonObject(jsonValue)
            .map(jsonObject -> {

                Optional<AttributeState> state = AttributeState.fromJsonValue(jsonObject.get("attributeState"));
                Optional<Integer> delay = JsonUtil.asInteger((JsonValue)jsonObject.get("delay"));

                return state
                    .map(attributeState -> new MacroAction(attributeState, delay.orElse(0)))
                    .orElse(null);
            });
    }
}
