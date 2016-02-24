package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class OverviewPlace extends Place {

    @Prefix("overview")
    public static class Tokenizer implements PlaceTokenizer<OverviewPlace> {

        @Override
        public OverviewPlace getPlace(String token) {
            return new OverviewPlace();
        }

        @Override
        public String getToken(OverviewPlace place) {
            return "";
        }
    }
}
