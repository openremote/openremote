/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2011, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.controller.event;

public class SwitchEvent extends Event<String> {

    public enum State {
        ON("on"),
        OFF("off");

        private String serializedFormat;

        State(String serializedStringFormat) {
            this.serializedFormat = serializedStringFormat;
        }

        public String serialize() {
            return serializedFormat;
        }
    }

    private String switchValue;
    private State switchState;

    public SwitchEvent(int sourceID, String sourceName, String switchValue, State switchState) {
        super(sourceID, sourceName);

        this.switchState = switchState;
        this.switchValue = switchValue;
    }

    public SwitchEvent(int sourceID, String sourceName, State switchState) {
        this(sourceID, sourceName, switchState.serialize(), switchState);
    }

    public State getState() {
        return switchState;
    }

    public SwitchEvent clone(String eventValue, SwitchEvent.State state) {
        if (eventValue == null) {
            eventValue = state.serialize();
        }

        return new SwitchEvent(getSourceID(), getSource(), eventValue, state);
    }

    @Override
    public SwitchEvent clone(String newValue) {
        if (newValue.equalsIgnoreCase(SwitchEvent.State.ON.serialize())) {
            return clone(null, SwitchEvent.State.ON);
        } else {
            return clone(null, SwitchEvent.State.OFF);
        }
    }

    @Override
    public String getValue() {
        return switchValue;
    }

    @Override
    public void setValue(String value) {
        this.switchValue = value;
    }

    @Override
    public String serialize() {
        return getValue();
    }

    @Override
    public boolean isEqual(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        SwitchEvent s = (SwitchEvent) o;

        return s.getSourceID().equals(this.getSourceID())
            && s.getSource().equals(this.getSource())
            && s.getValue().equals(this.getValue());
    }


    @Override
    public String toString() {
        return
            "Switch Event (ID = " + getSourceID() + ", Source = '" + getSource() +
                "', Switch Value = '" + getValue() + "', Switch State = " + getState() + ")";
    }

}

