package org.openremote.manager.client.flows;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class FlowsPlace extends Place {

    @Prefix("flows")
    public static class Tokenizer implements PlaceTokenizer<FlowsPlace> {

        @Override
        public FlowsPlace getPlace(String token) {
            return new FlowsPlace();
        }

        @Override
        public String getToken(FlowsPlace place) {
            return "";
        }
    }
}
