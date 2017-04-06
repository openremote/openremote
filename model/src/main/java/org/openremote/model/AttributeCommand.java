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
package org.openremote.model;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 *
 */
public class AttributeCommand {
    protected boolean cancel;
    protected boolean repeat;
    protected JsonValue value;
    public static final AttributeCommand CANCEL_ATTRIBUTE_COMMAND;

    static {
        AttributeCommand cancel = new AttributeCommand();
        cancel.setCancel(true);
        CANCEL_ATTRIBUTE_COMMAND = cancel;
    }

    public AttributeCommand() {
    }

    public AttributeCommand(boolean repeat) {
        this(repeat, false, null);
    }

    public AttributeCommand(JsonValue value) {
        this(false, false, value);
    }

    public AttributeCommand(boolean repeat, boolean cancel, JsonValue value) {
        this.repeat = repeat;
        this.cancel = cancel;
        this.value = value;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public JsonValue getValue() {
        return value;
    }

    public void setValue(JsonValue value) {
        this.value = value != null ? value : Json.createNull();
    }

    public JsonValue asJsonValue() {
        JsonObject jsonObject = Json.createObject();
        jsonObject.put("cancel", Json.create(cancel));
        jsonObject.put("repeat", Json.create(repeat));
        jsonObject.put("value", value);
        return jsonObject;
    }
}
