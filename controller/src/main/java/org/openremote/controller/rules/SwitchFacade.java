package org.openremote.controller.rules;

import org.openremote.controller.sensor.SensorState;
import org.openremote.controller.sensor.Sensor;
import org.openremote.controller.sensor.SwitchSensorState;

public class SwitchFacade extends SensorFacade<SwitchFacade.SwitchAdapter> {

    @Override
    public SwitchAdapter name(String sensorName) throws Exception {
        SensorState evt = sensorStateUpdate.getControllerContext().queryState(sensorName);

        if (evt instanceof Sensor.UnknownState) {
            evt = new SwitchSensorState(
                evt.getSensorID(),
                evt.getSensorName(),
                SwitchSensorState.State.OFF.serialize(),
                SwitchSensorState.State.OFF
            );
        }

        if (evt instanceof SwitchSensorState) {
            return new SwitchAdapter((SwitchSensorState) evt);
        } else {
            throw new Exception("Sensor is not switch type: " + sensorName);
        }
    }

    public class SwitchAdapter {

        private SwitchSensorState switchSensorState;

        private SwitchAdapter(SwitchSensorState switchSensorState) {
            this.switchSensorState = switchSensorState;
        }

        public void off() {
            off(null);
        }

        public void off(String value) {
            if (value == null) {
                value = SwitchSensorState.State.OFF.serialize();
            }
            SwitchSensorState newSwitchSensorState = switchSensorState.clone(value, SwitchSensorState.State.OFF);
            terminateAndReplaceWith(newSwitchSensorState);
        }

        public void on() {
            on(null);
        }

        public void on(String value) {
            if (value == null) {
                value = SwitchSensorState.State.ON.serialize();
            }
            SwitchSensorState newSwitchSensorState = switchSensorState.clone(value, SwitchSensorState.State.ON);
            terminateAndReplaceWith(newSwitchSensorState);
        }
    }
}

