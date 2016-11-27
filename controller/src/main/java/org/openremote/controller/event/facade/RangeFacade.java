package org.openremote.controller.event.facade;

import org.openremote.controller.event.RangeEvent;

import java.util.logging.Logger;

public class RangeFacade extends SingleValueEventFacade<RangeFacade.RangeAdapter, RangeEvent> {

    private static final Logger LOG = Logger.getLogger(RangeFacade.class.getName());

    @Override
    protected RangeEvent createDefaultEvent(int sourceID, String sourceName) {
        // TODO We drop the min/max here, not good
        return new RangeEvent(sourceID, sourceName, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    protected RangeAdapter createAdapter(RangeEvent event) {
        return new RangeAdapter(event);
    }

    public class RangeAdapter {

        private RangeEvent rangeEvent;

        private RangeAdapter(RangeEvent event) {
            this.rangeEvent = event;
        }

        public void value(int value) {
            if (value < rangeEvent.getMinValue()) {
                value = rangeEvent.getMinValue();
            } else if (value > rangeEvent.getMaxValue()) {
                value = rangeEvent.getMaxValue();
            }

            RangeEvent newRangeEventEvent = rangeEvent.clone(value);

            LOG.fine("Dispatching event (original value was: '" + value + "'): " + newRangeEventEvent);
            dispatchEvent(newRangeEventEvent);
        }
    }
}

