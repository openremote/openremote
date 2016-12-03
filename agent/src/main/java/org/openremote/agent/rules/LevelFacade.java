package org.openremote.agent.rules;

import org.openremote.agent.sensor.LevelSensorState;

public class LevelFacade extends SingleValueSensorFacade<LevelFacade.LevelAdapter, LevelSensorState> {

    @Override
    protected LevelSensorState createDefaultState(int sourceID, String sourceName) {
        return new LevelSensorState(sourceID, sourceName, 0);
    }

    @Override
    protected LevelAdapter createAdapter(LevelSensorState sensorState) {
        return new LevelAdapter(sensorState);
    }

    public class LevelAdapter {

        private LevelSensorState levelSensorState;

        private LevelAdapter(LevelSensorState levelSensorState) {
            this.levelSensorState = levelSensorState;
        }

        public void value(int value) {
            // TODO Duplicate max/min logic?!
            if (value < 0) {
                value = 0;
            } else if (value > 100) {
                value = 100;
            }

            LevelSensorState newLevelSensorState = levelSensorState.clone(value);
            terminateAndReplaceWith(newLevelSensorState);
        }
    }
}

