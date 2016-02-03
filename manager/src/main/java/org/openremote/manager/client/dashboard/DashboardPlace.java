package org.openremote.manager.client.dashboard;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class DashboardPlace extends Place {

    @Prefix("dashboard")
    public static class Tokenizer implements PlaceTokenizer<DashboardPlace> {

        @Override
        public DashboardPlace getPlace(String token) {
            return new DashboardPlace();
        }

        @Override
        public String getToken(DashboardPlace place) {
            // Well, otherwise it's "dashboard:null", so "dashboard:index" looks better!
            // I think this is a bug in AbstractPlaceHistoryMapper
            return "index";
        }
    }
}
