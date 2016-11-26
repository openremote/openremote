package org.openremote.controller.statuscache;

import org.openremote.controller.event.SingleValueEventFacade;
import org.openremote.controller.event.LevelEvent;

public class LevelFacade extends SingleValueEventFacade<LevelFacade.LevelAdapter, LevelEvent> {

    @Override
    protected LevelEvent createDefaultEvent(int sourceID, String sourceName) {
        return new LevelEvent(sourceID, sourceName, 0);
    }

    @Override
    protected LevelAdapter createAdapter(LevelEvent event) {
        return new LevelAdapter(event);
    }

    public class LevelAdapter {

        private LevelEvent levelEvent;

        private LevelAdapter(LevelEvent levelEvent) {
            this.levelEvent = levelEvent;
        }

        public void value(int value) {
            if (value < 0) {
                value = 0;
            } else if (value > 100) {
                value = 100;
            }

            LevelEvent newLevelEventEvent = levelEvent.clone(value);

            dispatchEvent(newLevelEventEvent);
        }
    }
}

