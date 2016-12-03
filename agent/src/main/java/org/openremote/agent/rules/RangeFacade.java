package org.openremote.agent.rules;

import org.openremote.agent.sensor.RangeSensorState;

public class RangeFacade extends SingleValueSensorFacade<RangeFacade.RangeAdapter, RangeSensorState> {

    @Override
    protected RangeSensorState createDefaultState(int sourceID, String sourceName) {
        // TODO We drop the min/max here? This leads to updates that don't restrict boundaries...
        return new RangeSensorState(sourceID, sourceName, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    protected RangeAdapter createAdapter(RangeSensorState sensorState) {
        return new RangeAdapter(sensorState);
    }

    public class RangeAdapter {

        final private RangeSensorState rangeSensorState;

        private RangeAdapter(RangeSensorState rangeSensorState) {
            this.rangeSensorState = rangeSensorState;
        }

        public void value(int value) {
            if (value < rangeSensorState.getMinValue()) {
                value = rangeSensorState.getMinValue();
            } else if (value > rangeSensorState.getMaxValue()) {
                value = rangeSensorState.getMaxValue();
            }

            RangeSensorState newRangeSensorState = rangeSensorState.clone(value);
            terminateAndReplaceWith(newRangeSensorState);
        }
    }
}

