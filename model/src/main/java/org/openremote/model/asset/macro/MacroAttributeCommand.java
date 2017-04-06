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

import elemental.json.*;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MacroAttributeCommand extends AttributeCommand {
    public static class Execution {
        protected List<AttributeState> actionOverrides;
        protected Integer delay;

        public Execution() {
        }

        public Execution(List<AttributeState> actionOverrides, Integer delay) {
            this.actionOverrides = actionOverrides;
            this.delay = delay;
        }

        public Execution(Integer delay) {
            this.delay = delay;
        }

        public Execution(List<AttributeState> actionOverrides) {
            this.actionOverrides = actionOverrides;
        }

        public List<AttributeState> getActionOverrides() {
            return actionOverrides;
        }

        public void setActionOverrides(List<AttributeState> actionOverrides) {
            this.actionOverrides = actionOverrides;
        }

        public Integer getDelay() {
            return delay;
        }

        public void setDelay(Integer delay) {
            this.delay = delay;
        }

        public JsonValue asJsonValue() {
            JsonObject jsonObject = Json.createObject();

            if (actionOverrides != null) {
                JsonArray jsonArray = Json.createArray();

                IntStream.range(0, actionOverrides.size())
                        .forEach(i -> jsonArray.set(i, actionOverrides.get(i).asJsonValue()));

                jsonObject.put("actionOverrides", jsonArray);
            } else {
                jsonObject.put("actionOverrides", Json.createNull());
            }

            jsonObject.put("delay", delay == null ? Json.createNull() : Json.create(delay));

            return jsonObject;
        }

        public static Execution fromJsonValue(JsonValue jsonValue) {
            if (!(jsonValue instanceof JsonObject)) {
                return null;
            }

            JsonObject jsonObject = (JsonObject)jsonValue;
            List<AttributeState> actionOverrides = null;
            Integer delay = null;

            if (jsonObject.hasKey("actionOverrides")) {
                JsonArray actionOverridesArray = jsonObject.hasKey("actionOverrides") && !jsonObject.get("actionOverrides").jsEquals(Json.createNull()) ? jsonObject.getArray("actionOverrides") : null;
                if (actionOverridesArray != null && actionOverridesArray.length() > 0) {
                    actionOverrides = new ArrayList<>(actionOverridesArray.length());
                    List<AttributeState> finalActionOverrides = actionOverrides;
                    IntStream.range(0, actionOverridesArray.length())
                            .forEach(i -> finalActionOverrides.add(i, new AttributeState(actionOverridesArray.get(i))));
                }
            }

            if (jsonObject.hasKey("delay")) {
                JsonValue value = jsonObject.get("delay");
                if (value != null  && !(value instanceof JsonNull)) {
                    delay = new Double(value.asNumber()).intValue();
                }
            }

            return new Execution(actionOverrides, delay);
        }
    }

    public MacroAttributeCommand() {
        this(false, false, (List<Execution>) null);
    }

    public MacroAttributeCommand(Execution execution) {
        this(false, execution);
    }

    public MacroAttributeCommand(boolean repeat, Execution execution) {
        this(repeat, false, Collections.singletonList(execution));
    }

    public MacroAttributeCommand(boolean repeat, boolean cancel, List<Execution> schedule) {
        super(repeat, cancel, scheduleAsJsonValue(schedule));
    }

    public List<Execution> getSchedule() {
        return scheduleFromJsonValue(getValue());
    }

    public void setSchedule(List<Execution> schedule) {
        setValue(scheduleAsJsonValue(schedule));
    }

    protected static JsonValue scheduleAsJsonValue(List<Execution> schedule) {
        if (schedule == null) {
            return Json.createNull();
        }

        JsonArray jsonArray = Json.createArray();
        IntStream.range(0, schedule.size())
                .forEach(i -> jsonArray.set(i, schedule.get(i).asJsonValue()));

        return jsonArray;
    }

    protected static List<Execution> scheduleFromJsonValue(JsonValue jsonValue) {
        if (!(jsonValue instanceof JsonArray)) {
            return null;
        }
        JsonArray jsonArray = (JsonArray)jsonValue;
        return IntStream.range(0, jsonArray.length())
                .mapToObj(i -> Execution.fromJsonValue(jsonArray.get(i)))
                .collect(Collectors.toList());
    }

    public JsonValue asJsonValue() {
        return super.asJsonValue();
    }

    public static MacroAttributeCommand fromJsonValue(JsonValue jsonValue) {
        if (!(jsonValue instanceof JsonObject)) {
            return null;
        }
        JsonObject jsonObject = (JsonObject)jsonValue;

        return new MacroAttributeCommand(jsonObject.getBoolean("repeat"), jsonObject.getBoolean("cancel"), scheduleFromJsonValue(jsonObject.getArray("value")));
    }
}
