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
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.AttributeState;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AssetMeta;

public class MacroAction {
    protected AttributeState attributeState;
    protected int delay;

    public MacroAction(JsonObject jsonObject) {
        attributeState = new AttributeState(jsonObject.getObject("attributeState"));
        delay = new Double(jsonObject.getNumber("delay")).intValue();
    }

    public MacroAction(AttributeState attributeState, int delay) {
        this.attributeState = attributeState;
        this.delay = delay;
    }

    public AttributeState getAttributeState() {
        return attributeState;
    }

    public void setAttributeState(AttributeState attributeState) {
        this.attributeState = attributeState;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public JsonValue asJsonValue() {
        JsonObject jsonObect = Json.createObject();
        jsonObect.put("attributeState", attributeState != null ? attributeState.asJsonValue() : Json.create(null));
        jsonObect.put("delay", Json.create(delay));
        return jsonObect;
    }

    public MetaItem asMetaItem() {
        return new MetaItem(AssetMeta.MACRO_ACTION, asJsonValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "attributeState='" + attributeState + '\'' +
                ", delay='" + delay + '\'' +
                '}';
    }
}
