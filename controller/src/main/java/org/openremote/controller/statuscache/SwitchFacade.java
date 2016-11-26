package org.openremote.controller.statuscache;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventFacade;
import org.openremote.controller.model.Sensor;
import org.openremote.controller.event.SwitchEvent;

public class SwitchFacade extends EventFacade {

    public SwitchAdapter name(String name) throws Exception {
        Event evt = eventContext.getStatusCache().queryStatus(name);

        if (evt instanceof Sensor.UnknownEvent) {
            evt = new SwitchEvent(
                evt.getSourceID(),
                evt.getSource(),
                SwitchEvent.State.OFF.serialize(),
                SwitchEvent.State.OFF
            );
        }

        if (evt instanceof SwitchEvent) {
            return new SwitchAdapter((SwitchEvent) evt);
        } else {
            throw new Exception("Sensor is not switch type: " + name);
        }
    }

    public class SwitchAdapter {

        private SwitchEvent switchEvent;

        private SwitchAdapter(SwitchEvent switchEvent) {
            this.switchEvent = switchEvent;
        }

        public void off() {
            off(null);
        }

        public void off(String eventValue) {
            if (eventValue == null) {
                eventValue = SwitchEvent.State.OFF.serialize();
            }
            SwitchEvent newSwitchEvent = switchEvent.clone(eventValue, SwitchEvent.State.OFF);
            dispatchEvent(newSwitchEvent);
        }

        public void on() {
            on(null);
        }

        public void on(String eventValue) {
            if (eventValue == null) {
                eventValue = SwitchEvent.State.ON.serialize();
            }
            SwitchEvent newSwitchEvent = switchEvent.clone(eventValue, SwitchEvent.State.ON);
            dispatchEvent(newSwitchEvent);
        }
    }
}

